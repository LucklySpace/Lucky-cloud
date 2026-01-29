package com.xy.lucky.server.service;


import com.xy.lucky.core.model.*;
import com.xy.lucky.server.domain.dto.ChatDto;

import java.util.Map;

public interface MessageService {

    IMSingleMessage sendSingleMessage(IMSingleMessage singleMessageDto);

    IMGroupMessage sendGroupMessage(IMGroupMessage groupMessageDto);

    IMGroupAction sendGroupAction(IMGroupAction groupActionDto);

    void sendVideoMessage(IMVideoMessage videoMessageDto);

    void recallMessage(IMessageAction dto);

    Map<Integer, Object> list(ChatDto chatDto);

}
