package com.xy.lucky.gateway.log;

import com.xy.lucky.gateway.utils.IPAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.LinkedHashSet;

/**
 * 全局网关日志过滤器
 * 职责：记录所有请求的入参、来源 IP、处理耗时及响应状态码。
 */
@Slf4j
@Component
public class GatewayLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            long duration = (startTime != null) ? (System.currentTimeMillis() - startTime) : -1;

            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String ip = IPAddressUtil.getIPAddress(request);
            String method = request.getMethod().toString();
            String path = request.getPath().value();
            int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

            log.info("[Gateway] {} {} {} {} - {}ms", status, method, path, ip, duration);

            if (log.isDebugEnabled()) {
                URI originalUri = getOriginalUri(exchange);
                log.debug("[Gateway Detail] Target: {}, Params: {}", originalUri, request.getQueryParams());
            }
        }));
    }

    private URI getOriginalUri(ServerWebExchange exchange) {
        LinkedHashSet<URI> uris = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        return (uris != null && !uris.isEmpty()) ? uris.iterator().next() : exchange.getRequest().getURI();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

