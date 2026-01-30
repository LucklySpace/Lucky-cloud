//package com.xy.lucky.gateway.filter;
//
//import com.alibaba.nacos.common.utils.JacksonUtils;
//import com.xy.lucky.gateway.config.GatewayAuthProperties;
//import com.xy.lucky.gateway.utils.ResponseUtil;
//import com.xy.lucky.gateway.utils.SignUtils;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//import org.springframework.util.StringUtils;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//
/// **
// * 接口签名验证过滤器
// * 职责：对关键写操作接口进行 App 级签名验证，防止数据被篡改。
// * 参数：appId, sign, nonce, timestamp
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ApiSignFilter implements GlobalFilter, Ordered {
//
//    private final GatewayAuthProperties properties;
//    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        GatewayAuthProperties.ApiSign config = properties.getSign();
//        if (!properties.isEnabled() || !config.isEnabled()) {
//            return chain.filter(exchange);
//        }
//
//        ServerHttpRequest request = exchange.getRequest();
//        // 签名通常针对写操作或敏感查询
//        if (HttpMethod.GET.equals(request.getMethod())) {
//            return chain.filter(exchange);
//        }
//
//        return DataBufferUtils.join(request.getBody())
//                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
//                .flatMap(dataBuffer -> {
//                    byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
//                    dataBuffer.read(bodyBytes);
//                    DataBufferUtils.release(dataBuffer);
//                    String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
//
//                    return validateSignature(exchange, bodyString)
//                            .flatMap(valid -> {
//                                if (Boolean.FALSE.equals(valid)) {
//                                    return ResponseUtil.writeJson(exchange, HttpStatus.BAD_REQUEST, "SIGNATURE_INVALID");
//                                }
//                                // 重新包装请求体，向下游透传
//                                ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
//                                    @Override
//                                    public Flux<DataBuffer> getBody() {
//                                        return Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes));
//                                    }
//                                };
//                                return chain.filter(exchange.mutate().request(mutatedRequest).build());
//                            });
//                });
//    }
//
//    private Mono<Boolean> validateSignature(ServerWebExchange exchange, String body) {
//        ServerHttpRequest request = exchange.getRequest();
//        Map<String, String> queryParams = request.getQueryParams().toSingleValueMap();
//
//        Map<String, Object> allParams = new HashMap<>();
//        if (!CollectionUtils.isEmpty(queryParams)) {
//            allParams.putAll(queryParams);
//        }
//
//        if (StringUtils.hasText(body) && isJsonRequest(request)) {
//            try {
//                Map<?, ?> jsonMap = JacksonUtils.toObj(body, Map.class);
//                if (jsonMap != null) {
//                    jsonMap.forEach((k, v) -> allParams.put(String.valueOf(k), v));
//                }
//            } catch (Exception e) {
//                log.warn("签名解析 JSON Body 失败");
//            }
//        }
//
//        String appId = String.valueOf(allParams.get("appId"));
//        String sign = String.valueOf(allParams.get("sign"));
//        String nonce = String.valueOf(allParams.get("nonce"));
//        String timestampStr = String.valueOf(allParams.get("timestamp"));
//
//        if (!StringUtils.hasText(appId) || !StringUtils.hasText(sign) || !StringUtils.hasText(nonce)) {
//            return Mono.just(false);
//        }
//
//        // 1. 时间戳校验
//        long now = System.currentTimeMillis() / 1000L;
//        long ts = parseLong(timestampStr);
//        if (Math.abs(now - ts) > properties.getSign().getExpireTimeSeconds()) {
//            log.warn("签名失效：时间戳超时 - {}", timestampStr);
//            return Mono.just(false);
//        }
//
//        // 2. 防重放校验
//        String nonceKey = "gw:sign:nonce:" + appId + ":" + nonce;
//        return reactiveStringRedisTemplate.opsForValue()
//                .setIfAbsent(nonceKey, "1", Duration.ofSeconds(properties.getSign().getExpireTimeSeconds()))
//                .flatMap(saved -> {
//                    if (Boolean.FALSE.equals(saved)) {
//                        log.warn("重复请求：Nonce 已存在 - {}", nonce);
//                        return Mono.just(false);
//                    }
//                    // 3. 计算签名校验
//                    return getSecret(appId).map(secret -> {
//                        String calculated = SignUtils.calculateSign(allParams, secret);
//                        return sign.equalsIgnoreCase(calculated);
//                    });
//                });
//    }
//
//    private Mono<String> getSecret(String appId) {
//        // 优先从缓存/配置中获取，此处模拟逻辑
//        return Mono.just("LUCKY_IM_DEFAULT_SECRET_" + appId);
//    }
//
//    private boolean isJsonRequest(ServerHttpRequest request) {
//        MediaType contentType = request.getHeaders().getContentType();
//        return contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType);
//    }
//
//    private long parseLong(String val) {
//        try {
//            return Long.parseLong(val);
//        } catch (Exception e) {
//            return 0;
//        }
//    }
//
//    @Override
//    public int getOrder() {
//        return -180; // 在认证之前进行签名校验
//    }
//}
