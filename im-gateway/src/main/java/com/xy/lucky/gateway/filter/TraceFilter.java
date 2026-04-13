package com.xy.lucky.gateway.filter;

import com.xy.lucky.gateway.plugin.GatewayPlugin;
import com.xy.lucky.gateway.plugin.GatewayPluginChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class TraceFilter implements GatewayPlugin {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public String getId() {
        return "trace";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public int getOrder() {
        return -1000;
    }

    @Override
    public Mono<Void> apply(ServerWebExchange exchange, GatewayPluginChain chain) {
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

}
