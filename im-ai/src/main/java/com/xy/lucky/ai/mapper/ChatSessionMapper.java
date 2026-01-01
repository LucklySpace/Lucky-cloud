package com.xy.lucky.ai.mapper;

import com.xy.lucky.ai.domain.po.ChatSessionPo;
import com.xy.lucky.ai.domain.vo.ChatSessionVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatSessionMapper {

    ChatSessionVo toVo(ChatSessionPo po);

}

