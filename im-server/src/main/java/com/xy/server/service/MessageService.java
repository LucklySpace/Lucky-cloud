package com.xy.server.service;


import com.xy.core.model.IMGroupMessage;
import com.xy.core.model.IMPrivateMessage;
import com.xy.core.model.IMVideoMessage;
import com.xy.domain.dto.ChatDto;
import com.xy.general.response.domain.Result;

import java.util.Map;


public interface MessageService {

    Result sendPrivateMessage(IMPrivateMessage privateMessageDto);

    Result sendGroupMessage(IMGroupMessage groupMessageDto);

    Result sendVideoMessage(IMVideoMessage videoMessageDto);

    //    List<T> singleCheck(ChatDto chatDto);
//
    Map<Integer, Object> list(ChatDto chatDto);

}

