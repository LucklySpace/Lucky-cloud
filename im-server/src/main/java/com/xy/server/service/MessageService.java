package com.xy.server.service;


import com.xy.domain.dto.ChatDto;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMPrivateMessageDto;
import com.xy.imcore.model.IMVideoMessageDto;
import com.xy.response.domain.Result;

import java.util.Map;


public interface MessageService {

    Result sendPrivateMessage(IMPrivateMessageDto privateMessageDto);

    Result sendGroupMessage(IMGroupMessageDto groupMessageDto);

    Result sendVideoMessage(IMVideoMessageDto videoMessageDto);

//    List<T> singleCheck(ChatDto chatDto);
//
    Map<Integer, Object> list(ChatDto chatDto);

}

