package com.xy.lucky.server.service;


import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMVideoMessage;
import com.xy.lucky.core.model.IMessageAction;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.general.response.domain.Result;

import java.util.Map;


public interface MessageService {

    Result sendSingleMessage(IMSingleMessage singleMessageDto);

    Result sendGroupMessage(IMGroupMessage groupMessageDto);

    Result sendVideoMessage(IMVideoMessage videoMessageDto);

    Result recallMessage(IMessageAction dto);

    Map<Integer, Object> list(ChatDto chatDto);

}

