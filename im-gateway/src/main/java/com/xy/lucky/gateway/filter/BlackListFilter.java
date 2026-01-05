package com.xy.lucky.gateway.filter;

import com.alibaba.nacos.common.utils.JacksonUtils;
import com.xy.lucky.gateway.utils.IPAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * IP 黑名单过滤器（兼容版）
 * <p>
 * 使用 Redis 的：
 * - keyPrefix + "ban:{ip}" 来记录被封禁的 IP（TTL = banSeconds）
 * - keyPrefix + "bloom:{date}" 使用 Redis Set 存放近期被封禁的 IP（用于快速存在性判断）
 * <p>
 * 说明：
 * - 原始实现尝试使用 RedisBloom 模块命令（BF.ADD / BF.EXISTS），但很多 Redis 环境并未安装模块，
 * 因此此版本使用 Redis Set（opsForSet）以保证跨环境兼容性。
 * - 若你环境中安装了 RedisBloom 并且希望使用它，可以采用 Lettuce 的 RedisModulesCommands 或专门的 Java 客户端来调用模块命令。
 */
@Slf4j
@Component
public class BlackListFilter implements GlobalFilter, Ordered {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Value("${lucky.gateway.ip-guard.enabled:true}")
    private boolean enabled;

    @Value("${lucky.gateway.ip-guard.window-seconds:5}")
    private long windowSeconds;

    @Value("${lucky.gateway.ip-guard.max-requests:12}")
    private long maxRequests;

    @Value("${lucky.gateway.ip-guard.ban-seconds:600}")
    private long banSeconds;

    @Value("${lucky.gateway.ip-guard.key-prefix:gw:ipguard:}")
    private String keyPrefix;

    @Value("${lucky.gateway.ip-guard.bloom.enabled:true}")
    private boolean bloomEnabled;

    // bloom 过期时间（小时）
    @Value("${lucky.gateway.ip-guard.bloom.ttl-hours:6}")
    private long bloomTtlHours;

    public BlackListFilter(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * 规范化路径，用于键生成。
     */
    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "_";
        }
        String cleaned = path.replace('/', ':');
        return cleaned.length() > 256 ? cleaned.substring(0, 256) : cleaned;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String ip = IPAddressUtil.getIPAddress(exchange.getRequest());
        if (!StringUtils.hasText(ip)) {
            log.warn("无法获取 IP 地址");
            return chain.filter(exchange);
        }

        // 获取访问路径
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        String safePath = normalizePath(path);
        String banKey = keyPrefix + "ban:" + ip;
        String counterKey = keyPrefix + "cnt:" + windowSeconds + ":" + ip + ":" + safePath;

        log.info("正在请求访问 IP: {}, 访问路径: {}", ip, safePath);

        // 判断ip 是否被封禁
        return isBanned(ip, banKey)
                .flatMap(banned -> {
                    if (banned) {
                        return writeJsonResponse(exchange, HttpStatus.FORBIDDEN, "IP 已被封禁");
                    }
                    return incrementCounter(counterKey)
                            .flatMap(current -> {
                                if (current == 1L) {
                                    // 首次出现时设定窗口过期
                                    return reactiveRedisTemplate.expire(counterKey, Duration.ofSeconds(windowSeconds))
                                            .thenReturn(current);
                                }
                                return Mono.just(current);
                            })
                            .flatMap(current -> {
                                if (current > maxRequests) {
                                    return banIp(ip, banKey)
                                            .then(writeJsonResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, "访问过于频繁，请稍后再试"));
                                }
                                return chain.filter(exchange);
                            });
                })
                .onErrorResume(ex -> {
                    log.warn("IP 限流过滤器异常：{}", ex.getMessage(), ex);
                    return chain.filter(exchange);
                });
    }

    /**
     * 递增请求计数器。
     *
     * @param counterKey 计数器键。
     * @return 当前计数值。
     */
    private Mono<Long> incrementCounter(String counterKey) {
        return reactiveRedisTemplate.opsForValue().increment(counterKey);
    }

    /**
     * 检查 IP 是否被封禁。
     * <p>
     * 优先检查 banKey（明确封禁）；若启用 bloomEnabled 则再检查按天的 set（近似快速判断）。
     */
    private Mono<Boolean> isBanned(String ip, String banKey) {
        // 先检查是否存在直接封禁标记（强约束）
        return reactiveRedisTemplate.hasKey(banKey)
                .defaultIfEmpty(false)
                .flatMap(exists -> {
                    // 存在直接封禁标记，则直接返回
                    if (exists) return Mono.just(true);
                    // 不存在直接封禁标记
                    if (!bloomEnabled) return Mono.just(false);
                    // bloomEnabled: 使用 Set 作为加速判断
                    String bloomKey = getBloomKey();

                    return reactiveRedisTemplate.opsForSet().isMember(bloomKey, ip)
                            .defaultIfEmpty(false);
                });
    }

    /**
     * 封禁 IP。
     * <p>
     * 1) 在 banKey 上设置 TTL
     * 2) 如果启用 bloomEnabled，则把 ip 添加到按日维护的 set（用于快速存在判断），并设置 set 的过期时间
     */
    private Mono<Void> banIp(String ip, String banKey) {
        Mono<Boolean> setBan = reactiveRedisTemplate.opsForValue()
                .set(banKey, "1", Duration.ofSeconds(banSeconds));

        if (!bloomEnabled) {
            return setBan.then();
        }

        String bloomKey = getBloomKey();
        // add 返回被新增的元素数量（Long）
        Mono<Long> addToSet = reactiveRedisTemplate.opsForSet().add(bloomKey, ip);

        Mono<Boolean> expireSet = reactiveRedisTemplate.expire(bloomKey, Duration.ofHours(bloomTtlHours))
                .defaultIfEmpty(true);

        // 先添加到集合，再确保集合有过期时间，最后设置封禁键
        return addToSet.then(expireSet).then(setBan.then());
    }

    /**
     * 获取当前 Bloom Set 键（每日一个）。
     */
    private String getBloomKey() {
        return keyPrefix + "bloom:" + LocalDate.now().format(DAY_FORMATTER);
    }

    /**
     * 写入 JSON 响应。
     */
    private Mono<Void> writeJsonResponse(ServerWebExchange exchange, HttpStatus status, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("code", status.value());
        payload.put("msg", msg);

        byte[] bytes;
        try {
            bytes = JacksonUtils.toJsonBytes(payload);
        } catch (Exception e) {
            log.error("JSON 序列化失败：{}", e.getMessage(), e);
            bytes = "{}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -200; // 高优先级
    }
}
