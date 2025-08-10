package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.dto.ChatDto;
import com.xy.domain.po.ImChatPo;
import com.xy.domain.vo.ChatVo;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_chat_set】的数据库操作Service
 */
public interface ImChatService extends IService<ImChatPo> {

    List<ImChatPo> list(String ownerId, Long sequence);

//    void read(ChatDto chatDto);
//
//    ChatVo create(ChatDto ChatDto);
//
//    ChatVo one(String fromId, String toId);
}

