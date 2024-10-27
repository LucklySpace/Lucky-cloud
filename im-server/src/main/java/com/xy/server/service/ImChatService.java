package com.xy.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.server.domain.dto.ChatDto;
import com.xy.server.domain.vo.ChatVo;
import com.xy.server.model.ImChat;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_chat_set】的数据库操作Service
 * @createDate 2024-03-17 01:33:59
 */
public interface ImChatService extends IService<ImChat> {

    List<ChatVo> list(ChatDto chatDto);

    void read(ChatDto chatDto);

    ChatVo create(ChatDto ChatDto);

    ChatVo one(String fromId, String toId);
}

