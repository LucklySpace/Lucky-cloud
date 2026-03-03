package com.xy.lucky.chat.domain.mapper;

import com.xy.lucky.chat.domain.dto.ChatDto;
import com.xy.lucky.chat.domain.vo.ChatVo;
import com.xy.lucky.domain.po.ImChatPo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 聊天会话相关实体映射
 */
@Mapper(componentModel = "spring")
public interface ChatBeanMapper {

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