package com.xy.lucky.gateway.filter;

import com.alibaba.nacos.common.utils.JacksonUtils;
import com.xy.lucky.gateway.utils.IPAddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 黑名单过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlackListFilter implements GlobalFilter, Ordered {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private static final String keyPrefix = "gw:ipguard:";

    private final RedissonClient redissonClient;

    private final ConcurrentHashMap<String, Boolean> bloomInited = new ConcurrentHashMap<>();

    @Value("${lucky.gateway.ip-guard.enabled:true}")
    private boolean enabled;

    @Value("${lucky.gateway.ip-guard.window-seconds:5}")
    private long windowSeconds;

    @Value("${lucky.gateway.ip-guard.max-requests:12}")
    private long maxRequests;

    @Value("${lucky.gateway.ip-guard.ban-seconds:600}")
    private long banSeconds;

    @Value("${lucky.gateway.ip-guard.bloom.enabled:true}")
    private boolean bloomEnabled;

    @Value("${lucky.gateway.ip-guard.bloom.error-rate:0.0001}")
    private double bloomErrorRate;

    @Value("${lucky.gateway.ip-guard.bloom.capacity:1000000}")
    private long bloomCapacity;

    @Value("${lucky.gateway.ip-guard.bloom.ttl-hours:3}")
    private long bloomTtlHours;

    /**
     * 规范路径
     *
     * @param path 路径
     * @return 规范路径
     */
    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "_";
        }
        String cleaned = path.replace('/', ':');
        return cleaned.length() > 256 ? cleaned.substring(0, 256) : cleaned;
    }

    /**
     * 获取 Bloom Filter 的 key
     *
     * @param keyPrefix 前缀
     * @return key
     */
    private static String bloomKeyForToday(String keyPrefix) {
        return keyPrefix + "bloom:" + LocalDate.now().format(DAY_FORMATTER);
    }

    /**
     * 写入 JSON 数据
     *
     * @param exchange ServerWebExchange
     * @param status   状态码
     * @param msg      消息
     * @return Mono<Void>
     */
    private static Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String msg) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("code", status.value());
        payload.put("message", msg);

        byte[] bytes;
        try {
            bytes = JacksonUtils.toJsonBytes(payload);
        } catch (Exception e) {
            bytes = new byte[0];
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String ip = IPAddressUtil.getIPAddress(exchange.getRequest());
        if (!StringUtils.hasText(ip)) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        String safePath = normalizePath(path);

        long safeWindowSeconds = Math.max(1, windowSeconds);
        long safeBanSeconds = Math.max(1, banSeconds);

        String banKey = keyPrefix + "ban:" + ip;
        String counterKey = keyPrefix + "cnt:" + safeWindowSeconds + ":" + ip + ":" + safePath;
        String bloomKey = bloomKeyForToday(keyPrefix);

        log.warn("处理访问请求 ip：{} 地址：{} 窗口：{} 封禁：{}", ip, safePath, safeWindowSeconds, safeBanSeconds);
        return isBanned(ip, banKey, bloomKey)
                .flatMap(banned -> {
                    if (Boolean.TRUE.equals(banned)) {
                        log.warn("IP 访问被封禁：{}", ip);
                        return writeJson(exchange, HttpStatus.FORBIDDEN, "IP 已被封禁");
                    }
                    return incrementCounter(counterKey, safeWindowSeconds)
                            .flatMap(current -> {
                                if (current != null && current > maxRequests) {
                                    log.warn("IP 访问过于频繁：{}", ip);
                                    return banIp(ip, banKey, bloomKey, safeBanSeconds)
                                            .then(writeJson(exchange, HttpStatus.TOO_MANY_REQUESTS, "访问过于频繁，请稍后再试"));
                                }
                                return chain.filter(exchange);
                            });
                })
                .onErrorResume(ex -> {
                    log.warn("IP 频控过滤器异常：{}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    /**
     * 递增计数器
     *
     * @param counterKey  计数器 key
     * @param windowSeconds 窗口时长
     * @return long
     */
    private Mono<Long> incrementCounter(String counterKey, long windowSeconds) {
        return Mono.fromCallable(() -> {
                    RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
                    long current = counter.incrementAndGet();
                    if (current == 1L) {
                        counter.expire(Duration.ofSeconds(windowSeconds));
                    }
                    return current;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 判断 IP 是否被封禁
     *
     * @param ip          IP 地址
     * @param banKey      封禁 key
     * @param bloomKey    Bloom Filter key
     * @return boolean
     */
    private Mono<Boolean> isBanned(String ip, String banKey, String bloomKey) {
        return Mono.fromCallable(() -> {
                    if (!bloomEnabled) {
                        return redissonClient.getBucket(banKey).isExists();
                    }
                    RBloomFilter<String> bloomFilter = ensureBloomFilter(bloomKey);
                    if (!bloomFilter.contains(ip)) {
                        return false;
                    }
                    return redissonClient.getBucket(banKey).isExists();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(false);
    }

    /**
     * 封禁 IP
     *
     * @param ip          IP 地址
     * @param banKey      封禁 key
     * @param bloomKey    Bloom Filter key
     * @param banSeconds  封禁时长
     * @return void
     */
    private Mono<Void> banIp(String ip, String banKey, String bloomKey, long banSeconds) {
        return Mono.fromRunnable(() -> {
                    RBucket<String> banBucket = redissonClient.getBucket(banKey);
                    banBucket.set("1", Duration.ofSeconds(banSeconds));

                    if (!bloomEnabled) {
                        return;
                    }
                    RBloomFilter<String> bloomFilter = ensureBloomFilter(bloomKey);
                    bloomFilter.add(ip);
                    bloomFilter.expire(Duration.ofHours(Math.max(1, bloomTtlHours)));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * 确保 Bloom Filter 存在
     *
     * @param bloomKey Bloom Filter 的 key
     * @return Bloom Filter
     */
    private RBloomFilter<String> ensureBloomFilter(String bloomKey) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(bloomKey);
        bloomInited.computeIfAbsent(bloomKey, k -> {
            bloomFilter.tryInit(bloomCapacity, bloomErrorRate);
            bloomFilter.expire(Duration.ofHours(Math.max(1, bloomTtlHours)));
            return true;
        });
        return bloomFilter;
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
