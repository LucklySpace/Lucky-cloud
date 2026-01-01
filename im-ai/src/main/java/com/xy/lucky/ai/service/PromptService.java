package com.xy.lucky.ai.service;

import com.xy.lucky.ai.domain.vo.ChatPromptVo;

import java.util.List;

public interface PromptService {
    List<ChatPromptVo> list();

    boolean add(ChatPromptVo chatPrompt);

    boolean update(ChatPromptVo chatPrompt);
    boolean delete(String id);
}
