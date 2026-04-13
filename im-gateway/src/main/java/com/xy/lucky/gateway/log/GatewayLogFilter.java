package com.xy.lucky.gateway.log;

import com.xy.lucky.gateway.plugin.GatewayPlugin;
import com.xy.lucky.gateway.plugin.GatewayPluginChain;
import com.xy.lucky.gateway.utils.IPAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.LinkedHashSet;

@Slf4j
@Component
public class GatewayLogFilter implements GatewayPlugin {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public String getId() {
        return "gateway-log";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    public Mono<Void> apply(ServerWebExchange exchange, GatewayPluginChain chain) {
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
}
