package com.xy.lucky.dubbo.web.api.database.chat;

import com.xy.lucky.domain.po.ImChatPo;

import java.util.List;

/**
 * 聊天Dubbo服务接口
 */
public interface ImChatDubboService {

    /**
     * 查询某个会话
     *
     * @param ownerId  所属人
     * @param toId     会话对象
     * @param chatType 会话类型
     * @return 会话信息
     */
    ImChatPo queryOne(String ownerId, String toId, Integer chatType);

    /**
     * 查询用户所有会话
     *
     * @param ownerId  所属用户id
     * @param sequence 时序
     */
    List<ImChatPo> queryList(String ownerId, Long sequence);

    /**
     * 插入会话信息
     *
     * @param chatPo 会话信息
     */
    Boolean creat(ImChatPo chatPo);

    /**
     * 更新会话信息
     *
     * @param chatPo 会话信息
     */
    Boolean modify(ImChatPo chatPo);

    /**
     * 创建或更新会话信息
     *
     * @param chatPo 会话信息
     */
    Boolean creatOrModify(ImChatPo chatPo);

    /**
     * 删除会话信息
     *
     * @param id 会话id
     */
    Boolean removeOne(String id);
}
