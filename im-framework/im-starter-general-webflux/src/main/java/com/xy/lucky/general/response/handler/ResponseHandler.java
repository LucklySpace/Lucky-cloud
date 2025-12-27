package com.xy.lucky.general.response.handler;

import com.xy.lucky.general.response.domain.Result;
import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class ResponseHandler extends ResponseBodyResultHandler {


    public ResponseHandler(List<HttpMessageWriter<?>> writers, RequestedContentTypeResolver resolver, ReactiveAdapterRegistry registry) {
        super(writers, resolver, registry);
    }

    @Override
    public boolean supports(HandlerResult result) {
        Class<?> type = result.getReturnType().resolve();
        if (type == null) {
            return false;
        }
        if (type == Void.TYPE || type == Void.class) {
            return false;
        }
        if (Result.class.isAssignableFrom(type)) {
            return false;
        }
        if (ResponseEntity.class.isAssignableFrom(type)) {
            return false;
        }
        return true;
    }

    @Override
    public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        Object value = result.getReturnValue();
        Publisher<?> body;
        if (value instanceof Mono) {
            body = ((Mono<?>) value)
                    .map(v -> v instanceof Result ? v : Result.success(v))
                    .switchIfEmpty(Mono.just(Result.success()));
        } else if (value instanceof Flux) {
            body = ((Flux<?>) value).collectList().map(Result::success);
        } else if (value == null) {
            body = Mono.just(Result.success());
        } else {
            if (value instanceof Result) {
                body = Mono.just(value);
            } else {
                body = Mono.just(Result.success(value));
            }
        }
        return writeBody(body, result.getReturnTypeSource(), exchange);
    }
}



