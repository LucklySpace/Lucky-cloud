package com.xy.lucky.server.service;


import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMVideoMessage;
import com.xy.lucky.core.model.IMessageAction;
import com.xy.lucky.domain.dto.ChatDto;
import reactor.core.publisher.Mono;

import java.util.Map;


public interface MessageService {

    Mono<IMSingleMessage> sendSingleMessage(IMSingleMessage singleMessageDto);

    Mono<IMGroupMessage> sendGroupMessage(IMGroupMessage groupMessageDto);

    Mono<Void> sendVideoMessage(IMVideoMessage videoMessageDto);

    Mono<Void> recallMessage(IMessageAction dto);

    Mono<Map<Integer, Object>> list(ChatDto chatDto);

}
