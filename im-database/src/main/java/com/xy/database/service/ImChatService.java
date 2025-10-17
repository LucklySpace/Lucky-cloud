package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImChatPo;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_chat_set】的数据库操作Service
 */
public interface ImChatService extends IService<ImChatPo> {

    List<ImChatPo> selectList(String ownerId, Long sequence);

    /**
     * 查询某个会话
     *
     * @param ownerId  所属人
     * @param toId     会话对象
     * @param chatType 会话类型
     * @return 会话信息
     */
    ImChatPo selectOne(String ownerId, String toId, Integer chatType);

    /**
     * 插入会话信息
     *
     * @param chatPo 会话信息
     */
    Boolean insert(ImChatPo chatPo);

    /**
     * 更新会话信息
     *
     * @param chatPo 会话信息
     */
    Boolean update(ImChatPo chatPo);

    /**
     * 删除会话信息
     *
     * @param id 会话id
     */
    Boolean deleteById(String id);
}

