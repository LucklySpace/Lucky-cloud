package com.xy.lucky.chat.service;


import com.xy.lucky.core.model.*;
import com.xy.lucky.chat.domain.dto.ChatDto;

import java.util.Map;

public interface MessageService {

    IMSingleMessage sendSingleMessage(IMSingleMessage singleMessageDto);

    IMGroupMessage sendGroupMessage(IMGroupMessage groupMessageDto);

    void sendVideoMessage(IMVideoMessage videoMessageDto);

    void recallMessage(IMessageAction dto);

    void sendGroupAction(IMGroupAction groupActionDto);

    Map<Integer, Object> list(ChatDto chatDto);

}
