package com.xy.lucky.chat.service;


import com.xy.lucky.chat.domain.dto.ChatDto;
import com.xy.lucky.core.model.*;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImSingleMessagePo;

import java.util.List;

public interface MessageService {

    IMSingleMessage sendSingleMessage(IMSingleMessage singleMessageDto);

    IMGroupMessage sendGroupMessage(IMGroupMessage groupMessageDto);

    void sendVideoMessage(IMVideoMessage videoMessageDto);

    void recallMessage(IMessageAction dto);

    void sendGroupAction(IMGroupAction groupActionDto);

    List<ImSingleMessagePo> singleList(ChatDto chatDto);

    List<ImGroupMessagePo> groupList(ChatDto chatDto);
}
