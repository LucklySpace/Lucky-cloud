//package com.xy.lucky.gateway.filter;
//
//import com.alibaba.nacos.common.utils.JacksonUtils;
//import com.xy.lucky.gateway.utils.SignUtils;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
// * 网关全局签名验证过滤器
// * <p>
// * 功能：
// * - 针对 /api/ 路径进行签名校验（appId/sign/nonce/timestamp）
// * - 防重放：使用 Redis 的 setIfAbsent + expire（非阻塞）
// * - 验签成功后将 body 缓存并注入下游请求，保证下游仍可读取请求体
// * <p>
// * 说明：
// * - 仅支持 JSON body 的 POST 签名（可按需扩展表单或其他 content-type）
// * - 若 Redis 异常或校验失败，返回 400 与错误信息
// */
//@Slf4j
//@Component
//public class ApiSignFilter implements GlobalFilter, Ordered {
//
//    @Value("${lucky.gateway.sign.enabled:false}")
//    private Boolean isEnable = false;
//
//    /**
//     * nonce 在 Redis 中默认过期秒数（防止重复利用）
//     */
//    @Value("${lucky.gateway.sign.expire-time:300}")
//    private Long expireTime;
//
//    @Resource
//    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
//
//    private static String getString(Map<String, Object> map, String key) {
//        if (map == null || key == null) return null;
//        Object v = map.get(key);
//        if (v == null) return null;
//        String s = String.valueOf(v);
//        return s == null ? null : s.trim();
//    }
//
//    /* -------------------- 辅助方法 -------------------- */
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getPath().value();
//
//        // 仅对 /api/ 路径生效
//        if (!path.contains("/api/")) {
//            return chain.filter(exchange);
//        }
//
//        // 禁用
//        if (!isEnable) {
//            return chain.filter(exchange);
//        }
//
//        HttpMethod method = request.getMethod();
//        MediaType contentType = request.getHeaders().getContentType();
//        Map<String, String> queryParams = request.getQueryParams().toSingleValueMap();
//
//        // 读取并缓存 body（如果存在），然后进行签名校验与防重放
//        return DataBufferUtils.join(request.getBody())
//                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
//                .flatMap(dataBuffer -> {
//                    byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
//                    dataBuffer.read(bodyBytes);
//                    DataBufferUtils.release(dataBuffer);
//                    String bodyString = new String(bodyBytes, StandardCharsets.UTF_8).trim();
//
//                    // 合并参数：queryParams + JSON body（仅当是 POST+JSON 时解析）
//                    Map<String, Object> allParams = new HashMap<>();
//                    if (!CollectionUtils.isEmpty(queryParams)) {
//                        allParams.putAll(queryParams);
//                    }
//                    //
//                    if (HttpMethod.POST.equals(method)
//                            && contentType != null
//                            && MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
//                            && StringUtils.hasText(bodyString)) {
//                        try {
//                            Map jsonMap = JacksonUtils.toObj(bodyString, Map.class);
//                            if (jsonMap != null) allParams.putAll(jsonMap);
//                        } catch (Exception e) {
//                            log.warn("JSON 解析失败: {}", e.getMessage());
//                            return badRequest(exchange, "JSON 解析失败");
//                        }
//                    }
//
//                    // 获取签名相关字段（安全获取，避免 "null" 字符串）
//                    String appId = getString(allParams, "appId");
//                    String sign = getString(allParams, "sign");
//                    String nonce = getString(allParams, "nonce");
//                    String timestamp = getString(allParams, "timestamp");
//
//                    if (!StringUtils.hasText(appId) || !StringUtils.hasText(sign)
//                            || !StringUtils.hasText(nonce) || !StringUtils.hasText(timestamp)) {
//                        return badRequest(exchange, "签名参数缺失");
//                    }
//
//                    long nowSec = System.currentTimeMillis() / 1000L;
//                    long ts;
//                    try {
//                        ts = Long.parseLong(timestamp);
//                    } catch (NumberFormatException e) {
//                        return badRequest(exchange, "timestamp 格式错误");
//                    }
//                    if (Math.abs(nowSec - ts) > expireTime) {
//                        return badRequest(exchange, "请求已过期");
//                    }
//
//                    // 防重放：非阻塞地在 Redis 中做 setIfAbsent + expire
//                    String nonceKey = "nonce_cache:" + appId + ":" + nonce;
//                    return reactiveRedisTemplate.opsForValue().setIfAbsent(nonceKey, "1")
//                            .flatMap(set -> {
//                                if (Boolean.FALSE.equals(set)) {
//                                    return badRequest(exchange, "重复请求被拒绝");
//                                }
//                                // 设置过期时间，确保 key 不会一直存在
//                                return reactiveRedisTemplate.expire(nonceKey, Duration.ofSeconds(expireTime))
//                                        .onErrorResume(e -> {
//                                            // expire 失败不要阻断请求（容错），仅记录日志
//                                            log.warn("设置 nonce 过期失败: {}", e.getMessage(), e);
//                                            return Mono.just(false);
//                                        })
//                                        .then(Mono.defer(() -> {
//                                            // 计算签名并校验
//                                            String secret = getAppSecret(appId);
//                                            String calculated = SignUtils.calculateSign(allParams, secret);
//                                            if (!equalsIgnoreCase(sign, calculated)) {
//                                                log.warn("签名验证失败：appId={}, provided={}, calculated={}", appId, sign, calculated);
//                                                return badRequest(exchange, "签名校验失败");
//                                            }
//                                            log.debug("签名校验通过：appId={}", appId);
//
//                                            // 创建可重复消费的 body Flux
//                                            Flux<DataBuffer> cachedBody = Flux.defer(() -> {
//                                                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bodyBytes);
//                                                return Mono.just(buffer);
//                                            });
//
//                                            // 构造带标记的下游请求（header + body decorator）
//                                            ServerHttpRequest mutatedRequest = request.mutate()
//                                                    .header("X-Verified-Signature", "true")
//                                                    .build();
//
//                                            ServerHttpRequestDecorator decorated = new ServerHttpRequestDecorator(mutatedRequest) {
//                                                @Override
//                                                public Flux<DataBuffer> getBody() {
//                                                    return cachedBody;
//                                                }
//                                            };
//
//                                            return chain.filter(exchange.mutate().request(decorated).build());
//                                        }));
//                            })
//                            .onErrorResume(ex -> {
//                                // Redis 异常时，允许通过或拒绝：此处选择记录并拒绝以保安全（也可以降级为允许）
//                                log.error("Redis 操作失败：{}", ex.getMessage(), ex);
//                                return badRequest(exchange, "内部错误");
//                            });
//                });
//    }
//
//    @Override
//    public int getOrder() {
//        return -100;
//    }
//
//    private String getAppSecret(String appId) {
//        // TODO: 从配置或数据库获取 appSecret（示例返回静态字符串）
//        return "secretFor_" + appId;
//    }
//
//    private Boolean equalsIgnoreCase(String s1, String s2) {
//        return s1 == null ? s2 == null : s1.equalsIgnoreCase(s2);
//    }
//
//    private Mono<Void> badRequest(ServerWebExchange exchange, String msg) {
//        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
//        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
//        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
//        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
//        return exchange.getResponse().writeWith(Mono.just(buffer));
//    }
//}
