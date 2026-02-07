package com.xy.lucky.logging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.rpc.api.logging.vo.LogRecordVo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    public WebSocketHandler logWebSocketHandler(ObjectMapper mapper, Sinks.Many<LogRecordVo> logSink) {
        return session -> {
            var flux = logSink.asFlux()
                    .map(record -> {
                        try {
                            return mapper.writeValueAsString(record);
                        } catch (Exception e) {
                            return "{\"error\":\"serialize\"}";
                        }
                    })
                    .map(session::textMessage);
            return session.send(flux).and(session.receive().then());
        };
    }

    @Bean
    public SimpleUrlHandlerMapping handlerMapping(WebSocketHandler logWebSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws", logWebSocketHandler);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public Sinks.Many<LogRecordVo> logSink() {
        return Sinks.many().multicast().onBackpressureBuffer(8192, false);
    }
}
