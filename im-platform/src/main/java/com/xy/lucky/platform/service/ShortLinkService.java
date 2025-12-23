package com.xy.lucky.platform.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xy.lucky.platform.domain.CacheValue;
import com.xy.lucky.platform.domain.po.ShortLinkPo;
import com.xy.lucky.platform.domain.vo.ShortLinkVo;
import com.xy.lucky.platform.exception.ShortLinkException;
import com.xy.lucky.platform.mapper.ShortLinkVoMapper;
import com.xy.lucky.platform.repository.ShortLinkRepository;
import com.xy.lucky.platform.shortlink.ShortLinkProperties;
import com.xy.lucky.platform.utils.MurmurHashUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 短链服务 Redis + Caffeine 二级缓存
 * <p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkProperties shortLinkProperties;
    private final ShortLinkVoMapper shortLinkVoMapper;
    private final StringRedisTemplate redis;

    // 异步线程池（固定大小，守护线程）
    private final ExecutorService asyncPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "short-link-async");
                t.setDaemon(true);
                return t;
            });

    // 本地缓存（Caffeine：高并发 LRU）
    private Cache<String, CacheValue> localCache;

    @PostConstruct
    void initLocalCache() {
        localCache = Caffeine.newBuilder()
                .maximumSize(shortLinkProperties.getCacheCapacity())
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    /**
     * 创建短链（去重、持久化、异步缓存）
     */
    @Transactional
    public ShortLinkVo createShortLink(ShortLinkVo request) {
        String originalUrl = Optional.ofNullable(request.getOriginalUrl()).map(String::trim)
                .orElseThrow(() -> new ShortLinkException("URL 不能为空"));

        validateHttpUrl(originalUrl);

        // 布隆过滤器快速检查可能存在
        if (bloomMightContain(originalUrl)) {
            Optional<ShortLinkPo> existed = shortLinkRepository.findTopByOriginalUrlOrderByCreateTimeDesc(originalUrl);
            if (existed.isPresent()) {
                ShortLinkPo po = existed.get();
                if (Boolean.TRUE.equals(po.getEnabled()) && (po.getExpireTime() == null || po.getExpireTime().isAfter(LocalDateTime.now()))) {
                    // 同步访问计数从 Redis
                    String redisKey = keyForCode(po.getShortCode());
                    String visitCountStr = (String) redis.opsForHash().get(redisKey, "visitCount");
                    po.setVisitCount(StringUtils.hasText(visitCountStr) ? Integer.parseInt(visitCountStr) : 0);
                    return shortLinkVoMapper.toVo(po);
                }
            }
        }

        // 计算过期时间 默认 7 天
        LocalDateTime expireTime = Optional.ofNullable(request.getExpireSeconds())
                .map(seconds -> LocalDateTime.now().plusSeconds(seconds))
                .orElse(LocalDateTime.now().plusSeconds(shortLinkProperties.getCacheTtlSeconds()));

        // 确定性生成短码，冲突时增加长度重试
        int len = shortLinkProperties.getDeterministicLength();
        String code = generateDeterministicCode(originalUrl, len);
        while (existsCode(code)) {
            len++;
            code = generateDeterministicCode(originalUrl, len);

            if (len > 15) {
                throw new ShortLinkException("短码生成失败");
            }
        }

        // 持久化
        ShortLinkPo po = ShortLinkPo.builder()
                .shortCode(code)
                .originalUrl(originalUrl)
                .shortUrl(buildShortUrl(code))
                .visitCount(0)
                .expireTime(expireTime)
                .enabled(true)
                .build();
        shortLinkRepository.save(po);

        // 异步写缓存和布隆
        asyncPool.submit(() -> {
            try {
                writeCache(po.getShortCode(), po);
                bloomAdd(po.getOriginalUrl());
            } catch (Exception ex) {
                log.warn("异步写缓存失败 code={}", po.getShortCode(), ex);
            }
        });

        return shortLinkVoMapper.toVo(po);
    }

    /**
     * 解析短码并重定向（多级缓存 + 异步计数）
     */
    public ResponseEntity<Void> redirect(String shortCode) {

        // 参数检查
        shortCode = Optional.ofNullable(shortCode).map(String::trim)
                .filter(StringUtils::hasText).orElse(null);


        if (!StringUtils.hasText(shortCode)) {
            return ResponseEntity.notFound().build();
        }

        // 1. 本地缓存
        CacheValue cv = localCache.getIfPresent(shortCode);
        if (cv != null) {
            if (isExpiredOrDisabled(cv)) {
                localCache.invalidate(shortCode);
                return ResponseEntity.status(HttpStatus.GONE).build();
            }
            incrementVisitCount(shortCode);
            return redirectTo(cv.originalUrl);
        }

        // 2. Redis 缓存
        String redisKey = keyForCode(shortCode);
        Map<Object, Object> entries = redis.opsForHash().entries(redisKey);
        if (!entries.isEmpty()) {
            cv = CacheValue.fromMap(entries);
            if (isExpiredOrDisabled(cv)) {
                redis.delete(redisKey);
                return ResponseEntity.status(HttpStatus.GONE).build();
            }
            localCache.put(shortCode, cv);
            incrementVisitCount(shortCode);
            return redirectTo(cv.originalUrl);
        }

        // 3. 数据库回退
        Optional<ShortLinkPo> opt = shortLinkRepository.findByShortCode(shortCode);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ShortLinkPo po = opt.get();
        if (!Boolean.TRUE.equals(po.getEnabled()) || (po.getExpireTime() != null && po.getExpireTime().isBefore(LocalDateTime.now()))) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        // 写缓存 + 计数
        writeCache(shortCode, po);
        incrementVisitCount(shortCode);
        return redirectTo(po.getOriginalUrl());
    }

    /**
     * 禁用短链（DB + 失效缓存）
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

    /**
     * 查询短链信息（优先缓存）
     */
    public ShortLinkVo info(String shortCode) {
        CacheValue cv = localCache.getIfPresent(shortCode);
        if (cv != null) {
            return buildVo(shortCode, cv);
        }

        String redisKey = keyForCode(shortCode);
        Map<Object, Object> entries = redis.opsForHash().entries(redisKey);
        if (!entries.isEmpty()) {
            cv = CacheValue.fromMap(entries);
            localCache.put(shortCode, cv);
            return buildVo(shortCode, cv);
        }

        ShortLinkPo po = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortLinkException("短码不存在"));
        writeCache(shortCode, po);
        return shortLinkVoMapper.toVo(po);
    }


    /**
     * 重定向
     */
    private ResponseEntity<Void> redirectTo(String url) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    /**
     * 构建短链信息
     */
    private ShortLinkVo buildVo(String shortCode, CacheValue cv) {
        return ShortLinkVo.builder()
                .shortCode(shortCode)
                .shortUrl(buildShortUrl(shortCode))
                .originalUrl(cv.originalUrl)
                .visitCount(Optional.ofNullable(cv.visitCount).orElse(0))
                .expireTime(cv.expireTime)
                .enabled(cv.enabled)
                .build();
    }

    /**
     * 写缓存
     */
    private void writeCache(String code, ShortLinkPo po) {
        CacheValue cv = new CacheValue(po.getOriginalUrl(), po.getExpireTime(), po.getEnabled(), po.getVisitCount());
        localCache.put(code, cv);

        String redisKey = keyForCode(code);
        Map<String, String> map = new HashMap<>();
        map.put("originalUrl", po.getOriginalUrl());
        map.put("expireEpoch", po.getExpireTime() == null ? "" : String.valueOf(po.getExpireTime().toEpochSecond(ZoneOffset.UTC)));
        map.put("enabled", String.valueOf(po.getEnabled()));
        map.put("visitCount", String.valueOf(Optional.ofNullable(po.getVisitCount()).orElse(0)));
        redis.opsForHash().putAll(redisKey, map);

        long ttl = shortLinkProperties.getCacheTtlSeconds();
        if (ttl > 0) {
            redis.expire(redisKey, ttl, TimeUnit.SECONDS);
        }
    }

    /**
     * 检测短码是否存在（缓存 + DB）
     */
    private boolean existsCode(String code) {
        return redis.hasKey(keyForCode(code)) || shortLinkRepository.existsByShortCode(code);
    }

    /**
     * 生成 deterministic code
     */
    private String generateDeterministicCode(String url, int len) {
        return MurmurHashUtils.createAuto(url).substring(0, Math.min(len, MurmurHashUtils.createAuto(url).length()));
    }

    /**
     * 验证 HTTP URL
     */
    private void validateHttpUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new ShortLinkException("仅支持 http/https URL");
            }
        } catch (Exception e) {
            throw new ShortLinkException("URL 非法");
        }
    }

    /**
     * 递增访问次数
     * @param shortCode 短码
     */
    private void incrementVisitCount(String shortCode) {
        String redisKey = keyForCode(shortCode);
        try {
            long visitCount = redis.opsForHash().increment(redisKey, "visitCount", 1);

            // 更新本地缓存
            localCache.asMap().computeIfPresent(shortCode, (k, cv) -> {
                cv.visitCount = (int) visitCount;
                return cv;
            });

            long ttl = shortLinkProperties.getCacheTtlSeconds();
            if (ttl > 0) {
                redis.expire(redisKey, ttl, TimeUnit.SECONDS);
            }

            // 批量落库（每 10 次）
            if (visitCount % 10 == 0) {
                asyncPool.submit(() -> flushVisitCount(shortCode, (int) visitCount));
            }
        } catch (Exception e) {
            asyncPool.submit(() -> flushVisitCount(shortCode, null)); // 异常时异步 +1
        }
    }

    /**
     * 异步落库访问次数
     */
    private void flushVisitCount(String shortCode, Integer visitCount) {
        try {
            shortLinkRepository.findByShortCode(shortCode).ifPresent(po -> {
                if (visitCount != null) {
                    po.setVisitCount(visitCount);
                } else {
                    po.setVisitCount(Optional.ofNullable(po.getVisitCount()).orElse(0) + 1);
                }
                shortLinkRepository.save(po);
            });
        } catch (Exception ex) {
            log.warn("落库访问计数失败 code={}", shortCode, ex);
        }
    }

    /**
     * 构造短链
     */
    private String buildShortUrl(String code) {
        String domain = shortLinkProperties.getDomain();
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return (domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain) + "/r/" + code;
    }

    // ----------------------- 布隆过滤器 -----------------------

    /**
     * 布隆过滤器 添加
     * @param value 待添加的值
     */
    private void bloomAdd(String value) {
        for (int seed : shortLinkProperties.getBLOOM_SEEDS()) {
            int offset = bloomHash(value, seed);
            redis.opsForValue().setBit(shortLinkProperties.getBLOOM_KEY(), offset, true);
        }
    }

    /**
     * 布隆过滤器 查询是否存在
     * @param value 待查询的值
     * @return 是否存在
     */
    private boolean bloomMightContain(String value) {
        for (int seed : shortLinkProperties.getBLOOM_SEEDS()) {
            int offset = bloomHash(value, seed);
            Boolean bit = redis.opsForValue().getBit(shortLinkProperties.getBLOOM_KEY(), offset);
            if (!Boolean.TRUE.equals(bit)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 布隆过滤器 Hash
     * @param value 待Hash的值
     * @param seed 随机种子
     * @return Hash结果
     */
    private int bloomHash(String value, int seed) {
        int result = 0;
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : data) {
            result = result * seed + Byte.toUnsignedInt(b);
        }
        return Math.abs(result % shortLinkProperties.getBLOOM_SIZE());
    }

    /**
     * 缓存 Key
     *
     * @param code 短码
     * @return 缓存 Key
     */
    private String keyForCode(String code) {
        return String.format(shortLinkProperties.getKEY_CODE(), code);
    }

    /**
     * 判断缓存是否过期或者禁用
     * @param cv 缓存值对象
     * @return 是否过期或者禁用
     */
    private boolean isExpiredOrDisabled(CacheValue cv) {
        return !cv.enabled || (cv.expireTime != null && cv.expireTime.isBefore(LocalDateTime.now()));
    }
}