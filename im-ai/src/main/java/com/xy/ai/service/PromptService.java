package com.xy.ai.service;

import com.xy.ai.domain.ChatPrompt;

import java.util.List;

public interface PromptService {

    List<ChatPrompt> list();

    boolean add(ChatPrompt chatPrompt);

    boolean update(ChatPrompt chatPrompt);

    boolean delete(String id);

}
