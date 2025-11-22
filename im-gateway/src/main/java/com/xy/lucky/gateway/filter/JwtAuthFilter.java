//package com.xy.im.filter;
//
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//public class JwtAuthFilter implements GlobalFilter, Ordered {
//    private static final String JWT_TOKEN_HEADER = "Authorization";
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // 从请求头获取 JWT 令牌
//        String token = exchange.getRequest().getHeaders().getFirst(JWT_TOKEN_HEADER);
//
//        if (token == null || !token.startsWith("Bearer ")) {
//            // 无令牌或格式错误，返回 401 未认证
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        // 验证令牌逻辑（此处简化，实际需对接认证中心）
//        boolean isValid = validateToken(token.split(" ")[1]);
//        if (!isValid) {
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        // 令牌有效，放行请求
//        return chain.filter(exchange);
//    }
//
//    private boolean validateToken(String token) {
//        // 调用 JWT 解析库验证（如 jjwt）
//        return true;
//    }
//
//    @Override
//    public int getOrder() {
//        return -100; // 优先级：数值越小，执行越早
//    }
//}
