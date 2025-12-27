package com.xy.lucky.domain.mapper;

import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.po.ImChatPo;
import com.xy.lucky.domain.vo.ChatVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 聊天会话相关实体映射
 */
@Mapper
public interface ChatBeanMapper {

    ChatBeanMapper INSTANCE = Mappers.getMapper(ChatBeanMapper.class);

    /**
     * ImChatPo -> ChatVo
     */
    ChatVo toChatVo(ImChatPo imChatPo);

    /**
     * ChatDto -> ImChatPo
     */
    ImChatPo toImChatPo(ChatDto chatDto);

    /**
     * ImChatPo -> ChatDto
     */
    ChatDto toChatDto(ImChatPo imChatPo);

    /**
     * List<ImChatPo> -> List<ChatVo>
     */
    List<ChatVo> toChatVoList(List<ImChatPo> imChatPos);
}