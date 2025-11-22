package com.xy.dubbo.api.database.message;

import com.xy.domain.po.ImGroupMessagePo;
import com.xy.domain.po.ImGroupMessageStatusPo;

import java.util.List;

public interface ImGroupMessageDubboService {

    /**
     * 查询群组消息列表
     *
     * @param groupId  群组ID
     * @param sequence 消息序号
     * @return 群组消息列表
     */
    List<ImGroupMessagePo> selectList(String groupId, Long sequence);

    /**
     * 查询群组消息
     *
     * @param messageId 消息ID
     * @return 群组消息
     */
    ImGroupMessagePo selectOne(String messageId);

    /**
     * 插入群组消息
     *
     * @param groupMessagePo 群组消息
     * @return 是否成功
     */
    boolean insert(ImGroupMessagePo groupMessagePo);

    /**
     * 批量插入群组消息
     *
     * @param groupMessagePoList 群组消息列表
     * @return 是否成功
     */
    boolean batchInsert(List<ImGroupMessageStatusPo> groupMessagePoList);

    /**
     * 更新群组消息
     *
     * @param groupMessagePo 群组消息
     * @return 是否成功
     */
    boolean update(ImGroupMessagePo groupMessagePo);

    /**
     * 删除群组消息
     *
     * @param messageId 群组消息ID
     * @return 是否成功
     */
    boolean deleteById(String messageId);

    /**
     * 查询群组消息阅读状态
     *
     * @param groupId 群组ID
     * @param userId  接收方ID
     * @return 群组消息阅读状态
     */
    ImGroupMessagePo last(String groupId, String userId);

    /**
     * 查询群组消息阅读状态
     *
     * @param groupId 群组ID
     * @param ownerId 群主ID
     * @param code    群组消息状态码
     * @return 群组消息阅读状态
     */
    Integer selectReadStatus(String groupId, String ownerId, Integer code);
}
