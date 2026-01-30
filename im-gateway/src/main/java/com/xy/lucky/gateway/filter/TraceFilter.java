package com.xy.lucky.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 分布式链路追踪过滤器
 * 职责：确保每个请求都携带 traceId，若请求头缺失则由网关生成并注入，传递至下游服务。
 */
@Slf4j
@Component
public class TraceFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);

        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            log.debug("生成新追踪 ID: {}", traceId);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(TRACE_ID_HEADER, traceId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        log.debug("沿用已有追踪 ID: {}", traceId);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 最优先级，确保日志记录能拿到 traceId
    }
}

