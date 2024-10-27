//package com.xy.im.filter;
//
//import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.cloud.gateway.route.Route;
//import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//public class CustomFilter implements GlobalFilter{
//
//
//    private final LoadBalancerClient loadBalancerClient;
//
//    public CustomFilter(LoadBalancerClient loadBalancerClient) {
//        this.loadBalancerClient = loadBalancerClient;
//    }
//
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        String serviceId = getServiceId(exchange);  // 获取服务ID（一般是URI的host部分）
//
//        ServiceInstance serviceInstance = loadBalancerClient.choose(serviceId);
//
//        if (serviceInstance != null) {
//            String targetIp = serviceInstance.getHost();
//            int targetPort = serviceInstance.getPort();
//            System.out.println("Forwarded Service IP: " + targetIp + ", Port: " + targetPort);
//
//            // 可以将 IP 和端口添加到请求头中，方便后续使用
//            exchange.getRequest().mutate()
//                    .header("X-Forwarded-Service-Ip", targetIp)
//                    .header("X-Forwarded-Service-Port", String.valueOf(targetPort))
//                    .build();
//        }
//
//        return chain.filter(exchange);
//    }
//
//    public String getServiceId(ServerWebExchange exchange) {
//        // 获得路由
//        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
//        // 获取服务的实际 ID（Nacos 注册的服务名）
//        String serviceId = route.getId();
//
//        if (serviceId != null && serviceId.startsWith("lb://")) {
//            // 提取 Nacos 服务 ID
//            serviceId = serviceId.replace("lb://", "");
//        }
//
//        return serviceId;
//    }
//
//}