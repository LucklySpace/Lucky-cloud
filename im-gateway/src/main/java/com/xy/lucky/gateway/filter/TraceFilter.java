package com.xy.lucky.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Component
public class TraceFilter implements GlobalFilter, Ordered {

    private static final String TRACEID = "traceid";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        List<String> traceIds = request.getHeaders().get(TRACEID);

        if (traceIds == null) {

            String traceid = UUID.randomUUID().toString();

            log.debug("没有traceId，生成一个{}", traceid);

            final ServerHttpRequest finalRequest = exchange.getRequest()
                    .mutate()
                    .header(TRACEID, traceid)
                    .build();
            return chain.filter(exchange.mutate().request(finalRequest).build());

        }
        log.debug("traceId,已经存在{}", traceIds.getFirst());

        return chain.filter(exchange);
    }


    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

