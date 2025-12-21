package com.xy.lucky.server.service;

import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.vo.ChatVo;

import java.util.List;

public interface ChatService {

    List<ChatVo> list(ChatDto chatDto);

    void read(ChatDto chatDto);

    ChatVo create(ChatDto ChatDto);

    ChatVo one(String ownerId, String toId);

}
