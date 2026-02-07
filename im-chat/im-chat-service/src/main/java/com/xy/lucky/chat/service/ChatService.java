package com.xy.lucky.chat.service;

import com.xy.lucky.chat.domain.dto.ChatDto;
import com.xy.lucky.chat.domain.vo.ChatVo;

import java.util.List;

public interface ChatService {

    List<ChatVo> list(ChatDto chatDto);

    void read(ChatDto chatDto);

    ChatVo create(ChatDto ChatDto);

    ChatVo one(String ownerId, String toId);

}
