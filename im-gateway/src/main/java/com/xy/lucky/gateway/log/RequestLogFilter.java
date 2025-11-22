package com.xy.lucky.gateway.log;


import com.xy.lucky.gateway.utils.IPAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Cloud Gateway 的全局日志过滤器。
 * 记录所有进入网关的请求日志信息，包括客户端 IP、请求方法、URI、请求头、请求参数和处理耗时。
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    /**
     * 存储请求开始时间的属性键名，用于计算请求耗时。
     */
    private static final String START_TIME_ATTR = "StartTime";

    /**
     * 全局过滤器的核心方法。记录请求开始时间，调用后续过滤器链，最后输出结构化日志。
     *
     * @param exchange 当前请求上下文
     * @param chain    过滤器链
     * @return Mono<Void> 表示请求处理完成的异步结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 记录请求开始时间
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        // 获取请求信息
        ServerHttpRequest request = exchange.getRequest();
        String ip = IPAddressUtil.getIPAddress(request); // 获取客户端 IP
        String method = request.getMethod().toString(); // 获取请求方法
        String uri = getOriginalRequestUrl(exchange); // 获取原始请求 URI（含参数）
        Map<String, String> headers = new ConcurrentHashMap<>(request.getHeaders().toSingleValueMap()); // 获取请求头（单值）
        Map<String, String> queryParams = new ConcurrentHashMap<>(request.getQueryParams().toSingleValueMap()); // 获取请求参数（单值）

        // 执行过滤器链并在最后记录日志
        return chain.filter(exchange).doFinally(signalType -> {
                    Long startTime = exchange.getAttribute(START_TIME_ATTR);
                    long duration = (startTime != null) ? System.currentTimeMillis() - startTime : -1;

                    // 异步记录日志，避免阻塞网关工作线程
                    Mono.fromRunnable(() -> {
                        log.info(
                                "\n" +
                                        "================= Gateway Request Log =================\n" +
                                        "Client IP     : {}\n" +
                                        "Method        : {}\n" +
                                        "URI           : {}\n" +
                                        "Duration      : {} ms\n" +
                                        "Headers       : {}\n" +
                                        "Query Params  : {}\n" +
                                        "======================================================",
                                ip, method, uri, duration, headers, queryParams
                        );
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                }
        );
    }

    /**
     * 获取原始请求 URI，包括查询参数。
     * 优先使用 Gateway 原始请求属性 GATEWAY_ORIGINAL_REQUEST_URL。
     *
     * @param exchange 当前请求上下文
     * @return 原始请求 URI 字符串
     */
    private String getOriginalRequestUrl(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        @SuppressWarnings("unchecked")
        LinkedHashSet<URI> uris = exchange.getRequiredAttribute(
                ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        URI requestUri = uris.stream().findFirst().orElse(request.getURI());
        // 拼接查询参数（若存在）
        String rawQuery = request.getURI().getRawQuery();
        return requestUri.toString() + (rawQuery != null ? "?" + rawQuery : "");
    }

    /**
     * 根据请求类型提取参数（只处理 query/form 参数，不读取 body 流）
     *
     * @param request ServerHttpRequest 请求对象
     * @return 参数键值对 Map
     */
    private Map<String, String> extractRequestParams(ServerHttpRequest request) {
        HttpMethod method = request.getMethod();
        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            return request.getQueryParams().toSingleValueMap();
        }

        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            // 默认也只取 queryParam（避免读取 body），如需读取 body，需使用 body cache 机制
            return request.getQueryParams().toSingleValueMap();
        }

        return Collections.emptyMap();
    }

    /**
     * 设置过滤器优先级为最低，确保最后执行（日志打印收尾）。
     *
     * @return 执行顺序值
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}