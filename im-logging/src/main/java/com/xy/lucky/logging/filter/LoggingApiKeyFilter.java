package com.xy.lucky.logging.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class LoggingApiKeyFilter implements WebFilter {

    @Value("${logging.api-key:}")
    private String apiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String uri = exchange.getRequest().getPath().value();
        boolean protect = uri.startsWith("/api/logs");
        if (protect && apiKey != null && !apiKey.isBlank()) {
            String header = exchange.getRequest().getHeaders().getFirst("X-Api-Key");
            if (header == null || !header.equals(apiKey)) {
                var response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
                var buffer = response.bufferFactory().wrap("unauthorized".getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }
        }
        return chain.filter(exchange);
    }
}
