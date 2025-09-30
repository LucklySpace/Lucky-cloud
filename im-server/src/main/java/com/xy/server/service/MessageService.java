package com.xy.server.service;


import com.xy.core.model.IMGroupMessage;
import com.xy.core.model.IMSingleMessage;
import com.xy.core.model.IMVideoMessage;
import com.xy.core.model.IMessageAction;
import com.xy.domain.dto.ChatDto;
import com.xy.general.response.domain.Result;

import java.util.Map;


public interface MessageService {

    Result sendSingleMessage(IMSingleMessage singleMessageDto);

    Result sendGroupMessage(IMGroupMessage groupMessageDto);

    Result sendVideoMessage(IMVideoMessage videoMessageDto);

    Result recallMessage(IMessageAction dto);

    Map<Integer, Object> list(ChatDto chatDto);

}

