//package com.xy.im.filter;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.xy.im.utils.IPAddressUtil;
//import com.xy.im.utils.RedisUtil;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.annotation.Order;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Order(0)
//@Component
//public class BlackListFilter implements GlobalFilter {
//
//    private static final int time = 5;
//    private static final int count = 12;
//
//    @Resource
//    private StringRedisTemplate redisTemplate;
//
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // 获取请求的url
//        ServerHttpRequest request = exchange.getRequest();
//
//        String url = request.getPath().toString();
//
//        String ip = IPAddressUtil.getIPAddress(request);
//
//        String key = "crazeidea:" + ip.replace("/", ".") + url;
//
//        String value =redisTemplate.boundValueOps(key).get();
//
//        if (!StringUtils.hasLength(value)) {
//            // 为空则插入新数据
//            redisUtil.saveExpired(key, "1", time, TimeUnit.SECONDS);
//            return chain.filter(exchange);
//        } else {
//            int countValue = Integer.parseInt(value);
//            if (countValue < count) {
//                // 没有超过就累加
//                long redisTime = redisUtil.getExpire(key);
//                redisUtil.saveExpired(key, String.valueOf(countValue + 1), Math.toIntExact(redisTime), TimeUnit.SECONDS);
//                return chain.filter(exchange);
//            } else {
//                // 超过访问次数
//                //String cou = get(ip);
//                //if (StringUtils.hasLength(cou)) {
//                //    setNoTime(ip, "1");
//                //} else {
//                //    int couValue = Integer.parseInt(cou);
//                //    if (couValue <= 5) {
//                //        setNoTime(ip, String.valueOf(couValue + 1));
//                //        return chain.filter(exchange);
//                //    } else {
//                //        // 超过访问次数 5次以上 进入黑名单
//                //        del(ip);
//                //    }
//                //}
//            }
//        }
//
//
//        ServerHttpResponse response = exchange.getResponse();
//        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
//        response.setStatusCode(HttpStatus.FORBIDDEN);
//
//        Map<String, Object> map = new HashMap<>();
//        map.put("code", HttpStatus.FORBIDDEN.value());
//        map.put("msg", "请求无效");
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        byte[] bytes;
//        try {
//            bytes = objectMapper.writeValueAsBytes(map);
//        } catch (JsonProcessingException e) {
//            bytes = new byte[0];
//            e.printStackTrace();
//        }
//        DataBuffer buffer = response.bufferFactory().wrap(bytes);
//        return response.writeWith(Mono.just(buffer));
//    }
//
//
//}
