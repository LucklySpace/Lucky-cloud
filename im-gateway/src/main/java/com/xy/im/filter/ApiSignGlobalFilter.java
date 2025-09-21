//package com.xy.im.filter;
//
//import com.alibaba.nacos.common.utils.StringUtils;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.xy.im.utils.SignUtils;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
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
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
/// **
// * 网关层全局签名验证过滤器
// */
//@Slf4j
//@Component
//public class ApiSignGlobalFilter implements GlobalFilter, Ordered {
//
//    private static final long EXPIRE_TIME = 5 * 60;
//
//    private static final ObjectMapper MAPPER = new ObjectMapper();
//
//    @Resource
//    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getPath().value();
//
//        // 非 /api/ 路径跳过
//        if (!path.contains("/api/")) {
//            return chain.filter(exchange);
//        }
//
//        HttpMethod method = request.getMethod();
//        MediaType contentType = request.getHeaders().getContentType();
//        Map<String, String> queryParams = request.getQueryParams().toSingleValueMap();
//
//        // 只读取，不修改
//        return DataBufferUtils.join(request.getBody()).flatMap(dataBuffer -> {
//            byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
//            dataBuffer.read(bodyBytes);
//            DataBufferUtils.release(dataBuffer);
//            String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
//
//            // 收集所有参数（Query + JSON）
//            Map<String, Object> allParams = new HashMap<>(queryParams);
//            if (HttpMethod.POST.equals(method)
//                    && contentType != null
//                    && MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
//                    && StringUtils.isNotBlank(bodyString)) {
//                try {
//                    Map<String, Object> jsonMap = MAPPER.readValue(bodyString, Map.class);
//                    allParams.putAll(jsonMap);
//                } catch (Exception e) {
//                    return handleError(exchange, "JSON 解析失败");
//                }
//            }
//
//            // 签名字段校验
//            String appId = String.valueOf(allParams.get("appId"));
//            String sign = String.valueOf(allParams.get("sign"));
//            String nonce = String.valueOf(allParams.get("nonce"));
//            String timestamp = String.valueOf(allParams.get("timestamp"));
//
//            if (StringUtils.isAnyBlank(appId, sign, nonce, timestamp)) {
//                return handleError(exchange, "签名参数缺失");
//            }
//
//            long now = System.currentTimeMillis() / 1000;
//            if (Math.abs(now - Long.parseLong(timestamp)) > EXPIRE_TIME) {
//                return handleError(exchange, "请求过期");
//            }
//
//            //重放攻击防护（推荐使用 Redis + nonceKey 判断是否重复）
//            String nonceKey = "nonce_cache:" + appId + ":" + nonce;
//
//            try {
//                Boolean exist = reactiveRedisTemplate.opsForValue().setIfAbsent(nonceKey, "1").toFuture().get();
//
//                if (Boolean.FALSE.equals(exist)) {
//                    return handleError(exchange, "重复请求被拒绝");
//                }
//
//            } catch (Exception e) {
//                return handleError(exchange, "获取数据异常");
//            }
//
//            // 本地签名计算
//            String calculatedSign = SignUtils.calculateSign(allParams, getAppSecret(appId));
//            if (!sign.equalsIgnoreCase(calculatedSign)) {
//                log.error("接口参数签名不一致，请求签名:{} 验证签名:{}", sign, calculatedSign);
//                return handleError(exchange, "签名校验失败");
//            }
//
//            log.info("签名校验成功");
//
//            // 构造缓存 body 的 request，使下游还能读取 body
//            Flux<DataBuffer> cachedBody = Flux.defer(() -> {
//                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bodyBytes);
//                return Mono.just(buffer);
//            });
//
//            ServerHttpRequest mutatedRequest = request.mutate()
//                    .header("X-Verified-Signature", "true") // 可选：给下游标记已校验
//                    .build();
//
//            ServerWebExchange mutatedExchange = exchange.mutate()
//                    .request(new ServerHttpRequestDecorator(mutatedRequest) {
//                        @Override
//                        public Flux<DataBuffer> getBody() {
//                            return cachedBody;
//                        }
//                    }).build();
//
//            return chain.filter(mutatedExchange);
//        });
//    }
//
//    private String getAppSecret(String appId) {
//        return "secretFor_yourAppId";
//    }
//
//    private Mono<Void> handleError(ServerWebExchange exchange, String message) {
//        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
//        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
//        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
//        return exchange.getResponse().writeWith(Mono.just(buffer));
//    }
//
//    @Override
//    public int getOrder() {
//        return -100; // 提前执行
//    }
//}
