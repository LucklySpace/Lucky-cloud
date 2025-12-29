package com.xy.lucky.ai.service;

import com.xy.lucky.ai.domain.vo.ChatSessionVo;

import java.util.List;

public interface ChatSessionService {

    List<ChatSessionVo> listByUser(String userId);

    ChatSessionVo getWithMessages(String sessionId);

    ChatSessionVo create(String userId);

    void delete(String sessionId);
}
