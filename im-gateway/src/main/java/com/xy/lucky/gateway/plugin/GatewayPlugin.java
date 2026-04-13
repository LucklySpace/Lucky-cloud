package com.xy.lucky.gateway.plugin;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface GatewayPlugin {

    String getId();

    String getVersion();

    int getOrder();

    default boolean isEnabledByDefault() {
        return true;
    }

    Mono<Void> apply(ServerWebExchange exchange, GatewayPluginChain chain);
}
