package com.xy.lucky.gateway.filter;

import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.gateway.config.GatewayAuthProperties;
import com.xy.lucky.gateway.plugin.GatewayPlugin;
import com.xy.lucky.gateway.plugin.GatewayPluginChain;
import com.xy.lucky.gateway.utils.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAuthFilter implements GatewayPlugin {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GatewayAuthProperties properties;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public String getId() {
        return "auth";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public int getOrder() {
        return -150;
    }

    @Override
    public Mono<Void> apply(ServerWebExchange exchange, GatewayPluginChain chain) {
        GatewayAuthProperties.Auth config = properties.getAuth();
        if (!properties.isEnabled() || !config.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (isIgnored(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            return ResponseUtil.writeJson(exchange, HttpStatus.UNAUTHORIZED, "MISSING_TOKEN");
        }

        if (!JwtUtil.validate(token)) {
            return ResponseUtil.writeJson(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
        }

        String userId = JwtUtil.getUsername(token);
        if (!StringUtils.hasText(userId)) {
            return ResponseUtil.writeJson(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_USER");
        }

        Mono<Boolean> blacklistCheck = config.isCheckBlacklistEnabled()
                ? reactiveStringRedisTemplate.hasKey(config.getBlacklistKeyPrefix() + token)
                : Mono.just(false);

        return blacklistCheck.flatMap(blacklisted -> {
            if (Boolean.TRUE.equals(blacklisted)) {
                return ResponseUtil.writeJson(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED");
            }
            return enforceReplayProtection(exchange, userId)
                    .then(Mono.defer(() -> {
                        ServerHttpRequest mutated = exchange.getRequest().mutate()
                                .header(config.getUserHeader(), userId)
                                .build();
                        return chain.filter(exchange.mutate().request(mutated).build());
                    }));
        }).onErrorResume(ex -> {
            log.error("网关认证异常: {}", ex.getMessage());
            return ResponseUtil.writeJson(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_SYSTEM_ERROR");
        });
    }

    private Mono<Void> enforceReplayProtection(ServerWebExchange exchange, String userId) {
        GatewayAuthProperties.Auth config = properties.getAuth();
        if (!config.isReplayProtectionEnabled()) {
            return Mono.empty();
        }

        String method = exchange.getRequest().getMethod().name();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return Mono.empty();
        }

        String nonce = exchange.getRequest().getHeaders().getFirst(config.getNonceHeader());
        String timestampValue = exchange.getRequest().getHeaders().getFirst(config.getTimestampHeader());

        if (!StringUtils.hasText(nonce) || !StringUtils.hasText(timestampValue)) {
            return ResponseUtil.writeJson(exchange, HttpStatus.BAD_REQUEST, "MISSING_NONCE_TIMESTAMP");
        }

        long timestamp = parseTimestamp(timestampValue);
        long nowSeconds = System.currentTimeMillis() / 1000L;
        if (timestamp < 0 || Math.abs(nowSeconds - timestamp) > config.getTimestampWindowSeconds()) {
            return ResponseUtil.writeJson(exchange, HttpStatus.BAD_REQUEST, "TIMESTAMP_OUT_OF_RANGE");
        }

        String nonceKey = "gw:auth:nonce:" + userId + ":" + nonce;
        return reactiveStringRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofSeconds(config.getNonceTtlSeconds()))
                .flatMap(saved -> Boolean.TRUE.equals(saved) ? Mono.empty() : ResponseUtil.writeJson(exchange, HttpStatus.BAD_REQUEST, "REPLAY_ATTACK_DETECTED"));
    }

    private long parseTimestamp(String value) {
        try {
            long ts = Long.parseLong(value);
            return ts > 1000000000000L ? ts / 1000L : ts;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String resolveToken(ServerHttpRequest request) {
        GatewayAuthProperties.Auth config = properties.getAuth();
        String header = request.getHeaders().getFirst(config.getHeader());
        if (StringUtils.hasText(header)) {
            return stripBearer(header);
        }
        String param = request.getQueryParams().getFirst(config.getAccessTokenParam());
        return StringUtils.hasText(param) ? stripBearer(param) : null;
    }

    private String stripBearer(String token) {
        GatewayAuthProperties.Auth config = properties.getAuth();
        String prefix = config.getBearerPrefix();
        if (StringUtils.hasText(prefix) && token.startsWith(prefix)) {
            return token.substring(prefix.length()).trim();
        }
        return token.trim();
    }

    private boolean isIgnored(String path) {
        return properties.getIgnore().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
