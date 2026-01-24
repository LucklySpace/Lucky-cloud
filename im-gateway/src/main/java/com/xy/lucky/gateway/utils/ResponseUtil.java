package com.xy.lucky.gateway.utils;

import com.alibaba.nacos.common.utils.JacksonUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关响应工具类
 */
public final class ResponseUtil {

    private ResponseUtil() {
    }

    /**
     * 向客户端写入 JSON 格式的错误响应
     */
    public static Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());

        byte[] bytes;
        try {
            bytes = JacksonUtils.toJsonBytes(body);
        } catch (Exception e) {
            bytes = "{\"code\":500,\"message\":\"Internal Error\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}

