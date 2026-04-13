package com.xy.lucky.gateway.filter;

import com.xy.lucky.gateway.config.GatewayAuthProperties;
import com.xy.lucky.gateway.plugin.GatewayPlugin;
import com.xy.lucky.gateway.plugin.GatewayPluginChain;
import com.xy.lucky.gateway.utils.IPAddressUtil;
import com.xy.lucky.gateway.utils.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlackListFilter implements GatewayPlugin {

    private static final String KEY_PREFIX = "im-gateway:ip:guard:";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GatewayAuthProperties properties;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public String getId() {
        return "ip-guard";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public int getOrder() {
        return -200;
    }

    @Override
    public Mono<Void> apply(ServerWebExchange exchange, GatewayPluginChain chain) {
        GatewayAuthProperties.IpGuard config = properties.getIpGuard();
        if (!properties.isEnabled() || !config.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (isIgnored(path)) {
            return chain.filter(exchange);
        }

        String ip = IPAddressUtil.getIPAddress(exchange.getRequest());
        if (!StringUtils.hasText(ip)) {
            return chain.filter(exchange);
        }

        String banKey = KEY_PREFIX + "ban:" + ip;
        return reactiveStringRedisTemplate.hasKey(banKey)
                .flatMap(banned -> {
                    if (Boolean.TRUE.equals(banned)) {
                        log.warn("拒绝访问：IP 已被封禁 - {}", ip);
                        return ResponseUtil.writeJson(exchange, HttpStatus.FORBIDDEN, "IP_BANNED");
                    }
                    return checkRateLimit(ip, path)
                            .flatMap(blocked -> Boolean.TRUE.equals(blocked)
                                    ? ResponseUtil.writeJson(exchange, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED")
                                    : chain.filter(exchange));
                })
                .onErrorResume(ex -> {
                    log.error("IP 频控校验异常: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private Mono<Boolean> checkRateLimit(String ip, String path) {
        GatewayAuthProperties.IpGuard config = properties.getIpGuard();
        String safePath = path.replace('/', ':');
        String counterKey = KEY_PREFIX + "cnt:" + ip + ":" + safePath;
        String banKey = KEY_PREFIX + "ban:" + ip;

        return reactiveStringRedisTemplate.opsForValue().increment(counterKey)
                .defaultIfEmpty(1L)
                .flatMap(current -> {
                    if (current != null && current == 1L) {
                        return reactiveStringRedisTemplate.expire(counterKey, Duration.ofSeconds(config.getWindowSeconds()))
                                .thenReturn(current);
                    }
                    return Mono.just(current);
                })
                .flatMap(current -> {
                    if (current != null && current > config.getMaxRequests()) {
                        return reactiveStringRedisTemplate.opsForValue()
                                .set(banKey, "1", Duration.ofSeconds(config.getBanSeconds()))
                                .thenReturn(true);
                    }
                    return Mono.just(false);
                });
    }

    private boolean isIgnored(String path) {
        return properties.getIgnore().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
