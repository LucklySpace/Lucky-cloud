package com.xy.lucky.gateway.filter;

import com.alibaba.nacos.common.utils.JacksonUtils;
import com.xy.lucky.gateway.config.GatewayAuthProperties;
import com.xy.lucky.gateway.plugin.GatewayPlugin;
import com.xy.lucky.gateway.plugin.GatewayPluginChain;
import com.xy.lucky.gateway.utils.ResponseUtil;
import com.xy.lucky.gateway.utils.SignUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSignFilter implements GatewayPlugin {

    private final GatewayAuthProperties properties;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public String getId() {
        return "api-sign";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public int getOrder() {
        return -180;
    }

    @Override
    public Mono<Void> apply(ServerWebExchange exchange, GatewayPluginChain chain) {
        GatewayAuthProperties.ApiSign config = properties.getSign();
        if (!properties.isEnabled() || !config.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        if (HttpMethod.GET.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bodyBytes);
                    DataBufferUtils.release(dataBuffer);
                    String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
                    return validateSignature(exchange, bodyString)
                            .flatMap(valid -> {
                                if (Boolean.FALSE.equals(valid)) {
                                    return ResponseUtil.writeJson(exchange, HttpStatus.BAD_REQUEST, "SIGNATURE_INVALID");
                                }
                                ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
                                    @Override
                                    public Flux<DataBuffer> getBody() {
                                        return Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes));
                                    }
                                };
                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                            });
                });
    }

    private Mono<Boolean> validateSignature(ServerWebExchange exchange, String body) {
        ServerHttpRequest request = exchange.getRequest();
        Map<String, String> queryParams = request.getQueryParams().toSingleValueMap();
        Map<String, Object> allParams = new HashMap<>();
        if (!CollectionUtils.isEmpty(queryParams)) {
            allParams.putAll(queryParams);
        }
        if (StringUtils.hasText(body) && isJsonRequest(request)) {
            try {
                Map<?, ?> jsonMap = JacksonUtils.toObj(body, Map.class);
                if (jsonMap != null) {
                    jsonMap.forEach((k, v) -> allParams.put(String.valueOf(k), v));
                }
            } catch (Exception e) {
                log.warn("签名解析 JSON Body 失败");
            }
        }

        String appId = valueOf(allParams.get("appId"));
        String sign = valueOf(allParams.get("sign"));
        String nonce = valueOf(allParams.get("nonce"));
        String timestampStr = valueOf(allParams.get("timestamp"));
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(sign) || !StringUtils.hasText(nonce)) {
            return Mono.just(false);
        }

        long now = System.currentTimeMillis() / 1000L;
        long ts = parseLong(timestampStr);
        if (Math.abs(now - ts) > properties.getSign().getExpireTimeSeconds()) {
            log.warn("签名失效：时间戳超时 - {}", timestampStr);
            return Mono.just(false);
        }

        String nonceKey = "gw:sign:nonce:" + appId + ":" + nonce;
        return reactiveStringRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofSeconds(properties.getSign().getExpireTimeSeconds()))
                .flatMap(saved -> {
                    if (Boolean.FALSE.equals(saved)) {
                        log.warn("重复请求：Nonce 已存在 - {}", nonce);
                        return Mono.just(false);
                    }
                    return getSecret(appId).map(secret -> {
                        if (!StringUtils.hasText(secret)) {
                            return false;
                        }
                        String calculated = SignUtils.calculateSign(allParams, secret);
                        return sign.equalsIgnoreCase(calculated);
                    });
                });
    }

    private Mono<String> getSecret(String appId) {
        String key = properties.getSign().getSecretKeyPrefix() + appId;
        return reactiveStringRedisTemplate.opsForValue().get(key).defaultIfEmpty("");
    }

    private boolean isJsonRequest(ServerHttpRequest request) {
        MediaType contentType = request.getHeaders().getContentType();
        return contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType);
    }

    private long parseLong(String val) {
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return 0;
        }
    }

    private String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
