package com.xy.lucky.platform.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xy.lucky.platform.cache.CacheValue;
import com.xy.lucky.platform.domain.po.ShortLinkPo;
import com.xy.lucky.platform.domain.vo.CreateShortLinkVo;
import com.xy.lucky.platform.domain.vo.ShortLinkVo;
import com.xy.lucky.platform.exception.ShortLinkException;
import com.xy.lucky.platform.mapper.ShortLinkVoMapper;
import com.xy.lucky.platform.repository.ShortLinkRepository;
import com.xy.lucky.platform.shortlink.ShortLinkProperties;
import com.xy.lucky.platform.utils.MurmurHashUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 短链服务（Redis + 本地 LRU 二级缓存）
 * <p>
 * 说明：
 * - Redis 用作共享缓存与原始 URL 去重（short:code:{code} 与 short:long:{sha256}）
 * - 本地缓存为小型线程安全 LRU，存储热点，避免短期内频繁访问 Redis
 * - 访问计数通过 Redis HINCRBY 增量，然后定期由后台任务批量落库（见 flushVisitCountToDb）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkProperties shortLinkProperties;
    private final ShortLinkVoMapper shortLinkVoMapper;
    private final StringRedisTemplate redis;

    /**
     * 异步任务池（用于异步落库/更新）
     */
    private final ExecutorService asyncPool = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "shortlink-async");
                t.setDaemon(true);
                return t;
            });

    // 本地缓存（Caffeine）
    private Cache<String, CacheValue> localCache;

    @PostConstruct
    void initLocalCache() {
        localCache = Caffeine.newBuilder()
                .maximumSize(Math.max(1, shortLinkProperties.getCacheCapacity()))
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    /**
     * 创建短链（去重、持久化、回写缓存）
     */
    @Transactional
    public ShortLinkVo createShortLink(CreateShortLinkVo request) {

        // 原始 URL
        String originalUrl = Optional.ofNullable(request.getOriginalUrl()).map(String::trim).orElse(null);

        // 基本 URL 验证
        validateHttpUrl(originalUrl);

        // 使用布隆过滤器判断是否可能已存在短链；若可能存在则回查数据库
        if (bloomMightContain(originalUrl)) {
            log.info("使用布隆过滤器判断已存在短链，url={}", originalUrl);
            Optional<ShortLinkPo> existed = shortLinkRepository.findTopByOriginalUrlOrderByCreateTimeDesc(originalUrl);
            if (existed.isPresent()) {
                ShortLinkPo po = existed.get();
                boolean valid = Boolean.TRUE.equals(po.getEnabled()) && (po.getExpireTime() == null || !po.getExpireTime().isBefore(LocalDateTime.now()));
                if (valid) {
                    log.info("短链已存在，直接返回 id={} code={}", po.getId(), po.getShortCode());
                    String redisKey = keyForCode(po.getShortCode());
                    // 获取访问次数
                    Object visitCountObj = redis.opsForHash().get(redisKey, "visitCount");
                    if (Objects.nonNull(visitCountObj)) {
                        po.setVisitCount(Integer.valueOf(visitCountObj.toString()));
                    }
                    return shortLinkVoMapper.toVo(po);
                }
                log.info("短链已存在，但已失效，id={} code={}", po.getId(), po.getShortCode());
            }
        }

        // 短链有效期 默认 7 天
        LocalDateTime expireTime = Objects.nonNull(request.getExpireSeconds()) ?
                LocalDateTime.now().plusSeconds(request.getExpireSeconds()) : LocalDateTime.now().plusSeconds(shortLinkProperties.getCacheTtlSeconds());

        // 短码
        String code;
        if (StringUtils.hasText(request.getCustomCode())) {
            // 自定义短码，需检查唯一性（Redis + DB）
            code = request.getCustomCode();
            if (existsCode(code)) throw new ShortLinkException("短码已存在");
        } else {
            // 优先确定性生成（Murmur128），长度由配置决定，若冲突再随机重试
            int len = Math.max(6, shortLinkProperties.getDeterministicLength());
            code = generateDeterministicCodeWithUser(originalUrl, request.getUserId(), len);
            if (existsCode(code)) {
                code = ensureUniqueCode(len, 10);
            }
        }

        // 保存到 DB（若唯一约束冲突，重试一次）
        ShortLinkPo saved;
        try {
            ShortLinkPo po = ShortLinkPo.builder()
                    .shortCode(code)
                    .originalUrl(originalUrl)
                    .shortUrl(buildShortUrl(code))
                    .visitCount(0)
                    .expireTime(expireTime)
                    .enabled(true)
                    .build();
            saved = shortLinkRepository.save(po);
        } catch (DataIntegrityViolationException e) {
            // 唯一冲突，退化到随机生成再保存
            log.warn("唯一约束冲突，尝试回退随机码", e);
            String fallback = ensureUniqueCode(shortLinkProperties.getDeterministicLength() + 1, 10);
            ShortLinkPo po = ShortLinkPo.builder()
                    .shortCode(fallback)
                    .originalUrl(originalUrl)
                    .shortUrl(buildShortUrl(fallback))
                    .visitCount(0)
                    .expireTime(expireTime)
                    .enabled(true)
                    .build();
            saved = shortLinkRepository.save(po);
        }

        // 异步回写 Redis & 本地缓存 & long->code 去重映射
        ShortLinkPo finalSaved = saved;
        asyncPool.submit(() -> {
            try {
                writeCache(finalSaved.getShortCode(), finalSaved);
                bloomAdd(finalSaved.getOriginalUrl());
            } catch (Exception ex) {
                log.warn("写缓存失败 code={}", finalSaved.getShortCode(), ex);
            }
        });

        log.info("短链创建成功 id={} code={} 短链有效期={}", saved.getId(), saved.getShortCode(), expireTime);
        return shortLinkVoMapper.toVo(saved);
    }

    /**
     * 解析短码并返回 302 重定向或相应状态
     */
    public ResponseEntity<Void> redirect(String shortCode) {

        // 短码
        shortCode = Optional.ofNullable(shortCode).map(String::trim).orElse(null);

        if (!StringUtils.hasText(shortCode)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        // 1) 本地缓存优先
        CacheValue cv = localCache.getIfPresent(shortCode);
        if (cv != null) {
            if (isExpiredOrDisabled(cv)) {
                localCache.invalidate(shortCode);
                return ResponseEntity.status(HttpStatus.GONE).build();
            }
            // 命中本地缓存：增加 Redis 计数并异步落库
            incrementVisitCount(shortCode);
            return redirectTo(cv.originalUrl);
        }

        // 2) Redis 缓存（共享热点）
        Map<Object, Object> map = redis.opsForHash().entries(keyForCode(shortCode));
        if (map != null && !map.isEmpty()) {
            CacheValue fromRedis = CacheValue.fromMap(map);
            if (isExpiredOrDisabled(fromRedis)) {
                redis.delete(keyForCode(shortCode));
                return ResponseEntity.status(HttpStatus.GONE).build();
            }
            // 填充本地缓存并计数
            localCache.put(shortCode, fromRedis);
            incrementVisitCount(shortCode);
            return redirectTo(fromRedis.originalUrl);
        }

        // 3) DB 回退 如果为空 则返回 404
        Optional<ShortLinkPo> opt = shortLinkRepository.findByShortCode(shortCode);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShortLinkPo po = opt.get();
        if (!Boolean.TRUE.equals(po.getEnabled()) || (po.getExpireTime() != null && po.getExpireTime().isBefore(LocalDateTime.now()))) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        // 写 Redis、本地缓存，计数
        writeCache(shortCode, po);
        incrementVisitCount(shortCode);
        return redirectTo(po.getOriginalUrl());
    }

    /**
     * 禁用短链（DB + 缓存）
     */
    @Transactional
    public void disable(String shortCode) {
        shortLinkRepository.findByShortCode(shortCode).ifPresent(po -> {
            po.setEnabled(false);
            shortLinkRepository.save(po);
            redis.delete(keyForCode(shortCode));
            localCache.invalidate(shortCode);
        });
    }

    // ----------------------- 辅助方法 -----------------------

    /**
     * 查询短链信息（优先本地 -> redis -> db）
     */
    public ShortLinkVo info(String shortCode) {
        CacheValue cv = localCache.getIfPresent(shortCode);
        if (cv != null) {
            return buildVo(shortCode, cv);
        }
        Map<Object, Object> map = redis.opsForHash().entries(keyForCode(shortCode));
        if (map != null && !map.isEmpty()) {
            CacheValue rcv = CacheValue.fromMap(map);
            localCache.put(shortCode, rcv);
            return buildVo(shortCode, rcv);
        }
        ShortLinkPo po = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortLinkException("短码不存在"));
        // fill redis/local for future
        writeCache(shortCode, po);
        return shortLinkVoMapper.toVo(po);
    }

    /**
     * 跳转重定向
     *
     * @param url 目标 URL
     */
    private ResponseEntity<Void> redirectTo(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
    }

    /**
     * 构建短链信息
     */
    private ShortLinkVo buildVo(String shortCode, CacheValue cv) {
        ShortLinkVo vo = new ShortLinkVo();
        vo.setShortCode(shortCode);
        vo.setShortUrl(buildShortUrl(shortCode));
        vo.setOriginalUrl(cv.originalUrl);
        vo.setVisitCount(cv.visitCount == null ? 0 : cv.visitCount);
        vo.setExpireTime(cv.expireTime);
        vo.setEnabled(cv.enabled);
        return vo;
    }

    /**
     * 将 DB 对象写入 Redis（hash）与本地缓存
     */
    private void writeCache(String code, ShortLinkPo po) {
        CacheValue cv = new CacheValue(po.getOriginalUrl(), po.getExpireTime(), Boolean.TRUE.equals(po.getEnabled()), po.getVisitCount());
        // local
        localCache.put(code, cv);
        // redis hash（字段均以字符串存储）
        Map<String, String> map = new HashMap<>();
        map.put("originalUrl", po.getOriginalUrl());
        map.put("expireEpoch", po.getExpireTime() == null ? "" : String.valueOf(po.getExpireTime().toEpochSecond(ZoneOffset.UTC)));
        map.put("enabled", String.valueOf(Boolean.TRUE.equals(po.getEnabled())));
        map.put("visitCount", String.valueOf(po.getVisitCount() == null ? 0 : po.getVisitCount()));
        String redisKey = keyForCode(code);
        redis.opsForHash().putAll(redisKey, map);
        // 设置 TTL 以便自动失效（可配置）
        if (getCacheTtlSeconds() > 0) {
            redis.expire(redisKey, getCacheTtlSeconds(), TimeUnit.SECONDS);
        }
    }

    /**
     * 从 Redis 或 DB 中验证短码是否存在（用于创建时的唯一性检查）
     */
    private boolean existsCode(String code) {
        // 快速 Redis 检查
        if (Boolean.TRUE.equals(redis.hasKey(keyForCode(code)))) return true;
        return shortLinkRepository.findByShortCode(code).isPresent();
    }

    /**
     * 生成确定性短码（Murmur128截断），带 userId 隔离
     */
    private String generateDeterministicCodeWithUser(String url, String userId, int len) {
        String full = MurmurHashUtils.createWithUser(url, userId);
        if (full.length() <= len) return full;
        return full.substring(0, len);
    }

    /**
     * 随机 Base62 唯一生成（尝试 maxRetry 次），失败则增长长度返回
     */
    private String ensureUniqueCode(int len, int maxRetry) {
        for (int i = 0; i < maxRetry; i++) {
            String c = randomBase62(len);
            if (!existsCode(c)) return c;
        }
        return randomBase62(len + 1);
    }

    /**
     * 随机 Base62 生成
     */
    private String randomBase62(int len) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(BASE62[r.nextInt(BASE62.length)]);
        return sb.toString();
    }

    /**
     * 验证 URL 是否 HTTP/HTTPS
     */
    private void validateHttpUrl(String url) {
        try {
            URI u = URI.create(url);
            String s = u.getScheme();
            if (s == null || !(s.equalsIgnoreCase("http") || s.equalsIgnoreCase("https"))) {
                throw new ShortLinkException("仅支持 http/https URL");
            }
        } catch (Exception e) {
            throw new ShortLinkException("url 非法");
        }
    }

    /**
     * 计数策略：向 Redis 原子自增。推荐由独立调度任务调用 flushVisitCountToDb() 将计数批量写回 DB。
     */
    private void incrementVisitCount(String shortCode) {
        try {

            String redisKey = keyForCode(shortCode);

            // 访问次数 +1
            Object visitCountObj = redis.opsForHash().increment(redisKey, "visitCount", 1);

            // 更新本地缓存
            CacheValue cacheValue = localCache.getIfPresent(shortCode);
            if (cacheValue != null) {
                cacheValue.visitCount = (cacheValue.visitCount == null ? 0 : cacheValue.visitCount) + 1;
            }

            // 设置 Redis TTL
            if (getCacheTtlSeconds() > 0) {
                redis.expire(redisKey, getCacheTtlSeconds(), TimeUnit.SECONDS);
            }

            // 每100次访问批量更新到数据库
            int visitCountInt = Integer.parseInt(visitCountObj.toString());

            if (visitCountInt % 10 == 0) {
                shortLinkRepository.findByShortCode(shortCode).ifPresent(po -> {
                    po.setVisitCount(visitCountInt);
                    shortLinkRepository.save(po);
                });
            }

        } catch (Exception e) {
            // 避免影响主流程（解析），降级记录到异步任务中或直接落 DB
            asyncPool.submit(() -> {
                try {
                    shortLinkRepository.findByShortCode(shortCode).ifPresent(po -> {
                        po.setVisitCount((po.getVisitCount() == null ? 0 : po.getVisitCount()) + 1);
                        shortLinkRepository.save(po);
                    });
                } catch (Exception ex) {
                    log.warn("回退更新访问计数失败 code={}", shortCode, ex);
                }
            });
        }
    }

    /**
     * 拼接生成短链接
     */
    private String buildShortUrl(String code) {
        String domain = shortLinkProperties.getDomain();
        if (!StringUtils.hasText(domain)) return null;
        String base = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        return base + "/r/" + code;
    }

    // ----------------------- Bloom 过滤器 -----------------------

    /**
     * 添加
     *
     * @param value 待添加的值
     */
    private void bloomAdd(String value) {
        for (int seed : shortLinkProperties.getBLOOM_SEEDS()) {
            int offset = bloomHash(value, seed);
            try {
                redis.opsForValue().setBit(shortLinkProperties.getBLOOM_KEY(), offset, true);
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * 判断是否包含某值
     *
     * @param value 待判断的值
     * @return 存在则返回 true，否则返回 false
     */
    private boolean bloomMightContain(String value) {
        for (int seed : shortLinkProperties.getBLOOM_SEEDS()) {
            int offset = bloomHash(value, seed);
            Boolean bit = null;
            try {
                bit = redis.opsForValue().getBit(shortLinkProperties.getBLOOM_KEY(), offset);
            } catch (Exception ignore) {
            }
            if (bit == null || !bit) return false;
        }
        return true;
    }

    /**
     * 计算 hash 值
     *
     * @param value 待计算 hash 值的字符串
     * @param seed  随机数种子
     * @return 随机值
     */
    private int bloomHash(String value, int seed) {
        int result = 0;
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : data) {
            result = result * seed + b;
        }
        return Math.abs(result % shortLinkProperties.getBLOOM_SIZE());
    }

    // ----------------------- 小工具 -----------------------

    private String keyForCode(String code) {
        return String.format(shortLinkProperties.getKEY_CODE(), code);
    }

    private Long getCacheTtlSeconds() {
        return shortLinkProperties.getCacheTtlSeconds();
    }

    private boolean isExpiredOrDisabled(CacheValue cv) {
        return !cv.enabled || (cv.expireTime != null && cv.expireTime.isBefore(LocalDateTime.now()));
    }
}
