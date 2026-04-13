package com.xy.lucky.gateway.plugin;

import com.xy.lucky.gateway.config.GatewayPluginProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PluginGatewayFilter implements GlobalFilter, Ordered {

    private final PluginRegistry pluginRegistry;
    private final GatewayPluginProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<PluginRuntime> runtimes = pluginRegistry.getRuntimes();
        return new InternalChain(runtimes, 0, chain).filter(exchange);
    }

    @Override
    public int getOrder() {
        return properties.getFilterOrder();
    }

    private record InternalChain(List<PluginRuntime> runtimes, int index, GatewayFilterChain terminalChain)
            implements GatewayPluginChain {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            if (index >= runtimes.size()) {
                return terminalChain.filter(exchange);
            }
            PluginRuntime runtime = runtimes.get(index);
            String path = exchange.getRequest().getPath().value();
            GatewayPluginChain next = new InternalChain(runtimes, index + 1, terminalChain);
            if (!runtime.matchesPath(path)) {
                return next.filter(exchange);
            }
            return runtime.invoke(exchange, next);
        }
    }
}
