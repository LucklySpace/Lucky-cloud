package com.xy.lucky.gateway.plugin;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface GatewayPluginChain {

    Mono<Void> filter(ServerWebExchange exchange);
}
