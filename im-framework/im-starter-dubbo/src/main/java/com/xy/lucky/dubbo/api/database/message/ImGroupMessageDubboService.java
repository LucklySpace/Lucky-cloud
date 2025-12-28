package com.xy.lucky.dubbo.api.database.message;

import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;

import java.util.List;

public interface ImGroupMessageDubboService {

    /**
     * 查询群组消息列表
     *
     * @param groupId  群组ID
     * @param sequence 消息序号
     * @return 群组消息列表
     */
    List<ImGroupMessagePo> queryList(String groupId, Long sequence);

    /**
     * 查询群组消息
     *
     * @param messageId 消息ID
     * @return 群组消息
     */
    ImGroupMessagePo queryOne(String messageId);

    /**
     * 插入群组消息
     *
     * @param groupMessagePo 群组消息
     * @return 是否成功
     */
    boolean creat(ImGroupMessagePo groupMessagePo);

    /**
     * 批量插入群组消息
     *
     * @param groupMessagePoList 群组消息列表
     * @return 是否成功
     */
    boolean creatBatch(List<ImGroupMessageStatusPo> groupMessagePoList);

    /**
     * 更新群组消息
     *
     * @param groupMessagePo 群组消息
     * @return 是否成功
     */
    boolean modify(ImGroupMessagePo groupMessagePo);

    /**
     * 删除群组消息
     *
     * @param messageId 群组消息ID
     * @return 是否成功
     */
    boolean removeOne(String messageId);

    /**
     * 查询群组消息阅读状态
     *
     * @param groupId 群组ID
     * @param userId  接收方ID
     * @return 群组消息阅读状态
     */
    ImGroupMessagePo queryLast(String groupId, String userId);

    /**
     * 查询群组消息阅读状态
     *
     * @param groupId 群组ID
     * @param ownerId 群主ID
     * @param code    群组消息状态码
     * @return 群组消息阅读状态
     */
    Integer queryReadStatus(String groupId, String ownerId, Integer code);
}
