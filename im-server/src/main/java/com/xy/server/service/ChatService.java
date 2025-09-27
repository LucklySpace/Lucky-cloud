package com.xy.server.service;

import com.xy.domain.dto.ChatDto;
import com.xy.domain.vo.ChatVo;
import com.xy.general.response.domain.Result;

import java.util.List;

public interface ChatService {

    List<ChatVo> list(ChatDto chatDto);

    Result read(ChatDto chatDto);

    ChatVo create(ChatDto ChatDto);

    ChatVo one(String fromId, String toId);

}
