package com.xy.lucky.gateway.log;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.LinkedHashSet;

/**
 * Spring Cloud Gateway 的全局响应日志过滤器。
 * 记录所有通过网关的响应日志信息，包括状态码、请求方法、URI、请求头、请求参数和处理耗时。
 */
@Slf4j
@Component
public class ResponseLogFilter implements GlobalFilter, Ordered {

    /**
     * 存储请求开始时间的属性键名，用于计算请求耗时。
     */
    private static final String START_TIME_ATTR = "StartTime";

    /**
     * 全局过滤器的核心方法。先执行后续过滤器链以捕获响应，然后输出结构化日志。
     *
     * @param exchange 当前请求上下文
     * @param chain    过滤器链
     * @return Mono<Void> 表示请求处理完成的异步结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 记录请求开始时间（与 RequestLogFilter 保持一致）
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().toString();            // 请求方法
        String uriPath = request.getPath().pathWithinApplication().value(); // 应用内路径

        // 执行过滤器链，处理完响应后记录日志
        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    // 计算处理耗时
                    Long startTime = exchange.getAttribute(START_TIME_ATTR);
                    long duration = (startTime != null) ? System.currentTimeMillis() - startTime : -1;

                    // 获取响应信息
                    ServerHttpResponse response = exchange.getResponse();
                    int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

                    // 构建完整请求 URL（含查询参数）
                    @SuppressWarnings("unchecked")
                    LinkedHashSet<URI> uris = (LinkedHashSet<URI>) exchange.getAttribute(
                            ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
                    URI originalUri = (uris != null && !uris.isEmpty()) ? uris.iterator().next() : request.getURI();
                    String fullUri = UriComponentsBuilder.fromUri(originalUri)
                            .queryParams(request.getQueryParams())
                            .build()
                            .toUriString();

                    // 异步记录日志，避免阻塞网关工作线程
                    Mono.fromRunnable(() -> {
                        // 输出结构化日志
                        log.info(
                                "\n" +
                                        "================ Gateway Response Log ================\n" +
                                        "Status Code   : {}\n" +
                                        "Method        : {}\n" +
                                        "URI           : {}\n" +
                                        "Duration      : {} ms\n" +
                                        "======================================================",
                                statusCode, method, fullUri, duration
                        );
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                })
        );
    }

    /**
     * 设置过滤器优先级为最高，确保在请求完成后最先记录响应日志。
     *
     * @return 执行顺序值
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}