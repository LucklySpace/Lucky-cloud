package com.xy.lucky.ai.mapper;

import com.xy.lucky.ai.domain.po.ChatPromptPo;
import com.xy.lucky.ai.domain.vo.ChatPromptVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatPromptMapper {

    ChatPromptVo toVo(ChatPromptPo po);

    ChatPromptPo toPo(ChatPromptVo vo);
}

