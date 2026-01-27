package com.xy.lucky.server.service;


import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMVideoMessage;
import com.xy.lucky.core.model.IMessageAction;
import com.xy.lucky.server.domain.dto.ChatDto;

import java.util.Map;

public interface MessageService {

    IMSingleMessage sendSingleMessage(IMSingleMessage singleMessageDto);

    IMGroupMessage sendGroupMessage(IMGroupMessage groupMessageDto);

    void sendVideoMessage(IMVideoMessage videoMessageDto);

    void recallMessage(IMessageAction dto);

    Map<Integer, Object> list(ChatDto chatDto);

}
