package com.xy.lucky.ai.service;

import com.xy.lucky.ai.domain.ChatPrompt;

import java.util.List;

public interface PromptService {

    List<ChatPrompt> list();

    boolean add(ChatPrompt chatPrompt);

    boolean update(ChatPrompt chatPrompt);

    boolean delete(String id);

}
