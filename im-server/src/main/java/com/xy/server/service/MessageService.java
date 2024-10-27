package com.xy.server.service;

import com.xy.server.domain.dto.ChatDto;

import java.util.List;
import java.util.Map;


public interface MessageService<T> {

    List<T> singleCheck(ChatDto chatDto);

    Map<Integer, Object> list(ChatDto chatDto);

}

