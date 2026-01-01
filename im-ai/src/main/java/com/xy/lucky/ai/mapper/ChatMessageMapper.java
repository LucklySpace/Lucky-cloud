package com.xy.lucky.ai.mapper;

import com.xy.lucky.ai.domain.po.ChatMessagePo;
import com.xy.lucky.ai.domain.vo.ChatMessageVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    ChatMessageVo toVo(ChatMessagePo po);

}

