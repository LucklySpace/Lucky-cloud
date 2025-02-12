package com.xy.im.filter;

import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class DynamicFallbackFilter extends AbstractGatewayFilterFactory<DynamicFallbackFilter.Config> {

    private final ReactiveDiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;

    public DynamicFallbackFilter(ReactiveDiscoveryClient discoveryClient, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.discoveryClient = discoveryClient;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) ->
                chain.filter(exchange)
                        .onErrorResume(error -> {
                            // 动态获取fallbackService参数，如果未传则使用配置的默认值
                            String fallbackServiceName = exchange.getRequest().getQueryParams()
                                    .getFirst("fallbackService");
                            if (fallbackServiceName == null) {
                                fallbackServiceName = config.getFallbackService();
                            }

                            // 获取指定服务的实例
                            return discoveryClient.getInstances(fallbackServiceName)
                                    .next()  // 选择一个可用实例
                                    .flatMap(serviceInstance -> {
                                        String fallbackUri = serviceInstance.getUri().toString() + exchange.getRequest().getPath();
                                        return webClientBuilder.build()
                                                .method(exchange.getRequest().getMethod())
                                                .uri(fallbackUri)
                                                .headers(headers -> headers.addAll(exchange.getRequest().getHeaders()))
                                                .body(exchange.getRequest().getBody(), String.class)
                                                .exchangeToMono(response -> {
                                                    exchange.getResponse().setStatusCode(response.statusCode());
                                                    return response.bodyToMono(String.class)
                                                            .flatMap(body -> exchange.getResponse()
                                                                    .writeWith(Mono.just(exchange.getResponse()
                                                                            .bufferFactory().wrap(body.getBytes()))));
                                                });
                                    })
                                    .switchIfEmpty(Mono.fromRunnable(() -> {
                                        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                                    }));
                        });
    }

    public static class Config {
        private String fallbackService;

        public String getFallbackService() {
            return fallbackService;
        }

        public void setFallbackService(String fallbackService) {
            this.fallbackService = fallbackService;
        }
    }
}
