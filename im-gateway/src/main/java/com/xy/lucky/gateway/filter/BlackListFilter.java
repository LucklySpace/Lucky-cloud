//package com.xy.lucky.gateway.filter;
//
//import com.xy.lucky.gateway.config.GatewayAuthProperties;
//import com.xy.lucky.gateway.utils.IPAddressUtil;
//import com.xy.lucky.gateway.utils.ResponseUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RAtomicLongReactive;
//import org.redisson.api.RBloomFilterReactive;
//import org.redisson.api.RBucketReactive;
//import org.redisson.api.RedissonReactiveClient;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.util.StringUtils;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//
/// **
// * IP 黑名单与频控过滤器
// * 职责：拦截黑名单 IP，并对正常请求进行频率限制，超限后自动封禁。
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class BlackListFilter implements GlobalFilter, Ordered {
//
//    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
//    private static final String KEY_PREFIX = "im-gateway:ip:guard:";
//    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
//
//    private final GatewayAuthProperties properties;
//    private final RedissonReactiveClient redissonReactiveClient;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        GatewayAuthProperties.IpGuard config = properties.getIpGuard();
//        if (!properties.isEnabled() || !config.isEnabled()) {
//            return chain.filter(exchange);
//        }
//
//        String path = exchange.getRequest().getPath().value();
//        if (isIgnored(path)) {
//            return chain.filter(exchange);
//        }
//
//        String ip = IPAddressUtil.getIPAddress(exchange.getRequest());
//        if (!StringUtils.hasText(ip)) {
//            return chain.filter(exchange);
//        }
//
//        String banKey = KEY_PREFIX + "ban:" + ip;
//        String bloomKey = KEY_PREFIX + "bloom:" + LocalDate.now().format(DAY_FORMATTER);
//
//        return isBanned(ip, banKey, bloomKey)
//                .flatMap(banned -> {
//                    if (Boolean.TRUE.equals(banned)) {
//                        log.warn("拒绝访问：IP 已被封禁 - {}", ip);
//                        return ResponseUtil.writeJson(exchange, HttpStatus.FORBIDDEN, "IP_BANNED");
//                    }
//                    return checkRateLimit(ip, path);
//                })
//                .then(chain.filter(exchange))
//                .onErrorResume(ex -> {
//                    log.error("IP 频控校验异常: {}", ex.getMessage());
//                    return chain.filter(exchange);
//                });
//    }
//
//    private Mono<Void> checkRateLimit(String ip, String path) {
//        GatewayAuthProperties.IpGuard config = properties.getIpGuard();
//        String safePath = path.replace('/', ':');
//        String counterKey = KEY_PREFIX + "cnt:" + ip + ":" + safePath;
//
//        RAtomicLongReactive counter = redissonReactiveClient.getAtomicLong(counterKey);
//        return counter.incrementAndGet()
//                .flatMap(current -> {
//                    if (current == 1) {
//                        return counter.expire(Duration.ofSeconds(config.getWindowSeconds())).thenReturn(current);
//                    }
//                    return Mono.just(current);
//                })
//                .flatMap(current -> {
//                    if (current > config.getMaxRequests()) {
//                        log.warn("触发封禁：IP 访问过快 - {}, path={}", ip, path);
//                        return banIp(ip);
//                    }
//                    return Mono.empty();
//                }).then();
//    }
//
//    private Mono<Boolean> isBanned(String ip, String banKey, String bloomKey) {
//        GatewayAuthProperties.IpGuard config = properties.getIpGuard();
//        if (config.getBloom().isEnabled()) {
//            RBloomFilterReactive<String> bloom = redissonReactiveClient.getBloomFilter(bloomKey);
//            return bloom.contains(ip).flatMap(present -> {
//                if (Boolean.FALSE.equals(present)) {
//                    return Mono.just(false);
//                }
//                return redissonReactiveClient.getBucket(banKey).isExists();
//            });
//        }
//        return redissonReactiveClient.getBucket(banKey).isExists();
//    }
//
//    private Mono<Void> banIp(String ip) {
//        GatewayAuthProperties.IpGuard config = properties.getIpGuard();
//        String banKey = KEY_PREFIX + "ban:" + ip;
//        String bloomKey = KEY_PREFIX + "bloom:" + LocalDate.now().format(DAY_FORMATTER);
//
//        RBucketReactive<String> bucket = redissonReactiveClient.getBucket(banKey);
//        Mono<Void> setBan = bucket.set("1", Duration.ofSeconds(config.getBanSeconds()));
//
//        if (config.getBloom().isEnabled()) {
//            RBloomFilterReactive<String> bloom = redissonReactiveClient.getBloomFilter(bloomKey);
//            return bloom.tryInit(config.getBloom().getCapacity(), config.getBloom().getErrorRate())
//                    .then(bloom.add(ip))
//                    .then(bloom.expire(Duration.ofHours(config.getBloom().getTtlHours())))
//                    .then(setBan);
//        }
//        return setBan;
//    }
//
//    private boolean isIgnored(String path) {
//        return properties.getIgnore().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
//    }
//
//    @Override
//    public int getOrder() {
//        return -200; // 最先执行 IP 校验
//    }
//}
