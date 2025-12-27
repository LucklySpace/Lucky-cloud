package com.xy.lucky.server.service;

import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.vo.ChatVo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ChatService {

    Mono<List<ChatVo>> list(ChatDto chatDto);

    Mono<Void> read(ChatDto chatDto);

    Mono<ChatVo> create(ChatDto ChatDto);

    Mono<ChatVo> one(String ownerId, String toId);

}
