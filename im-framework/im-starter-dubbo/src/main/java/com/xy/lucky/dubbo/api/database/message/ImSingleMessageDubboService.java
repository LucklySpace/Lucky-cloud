package com.xy.lucky.dubbo.api.database.message;

import com.xy.lucky.domain.po.ImSingleMessagePo;

import java.util.List;

public interface ImSingleMessageDubboService {

    /**
     * 查询单聊消息列表
     *
     * @param userId   用户ID
     * @param sequence 消息序列
     * @return 单聊消息列表
     */
    List<ImSingleMessagePo> selectList(String userId, Long sequence);

    /**
     * 查询单聊消息
     *
     * @param messageId 消息ID
     * @return 单聊消息
     */
    ImSingleMessagePo selectOne(String messageId);

    /**
     * 插入单聊消息
     *
     * @param singleMessagePo 单聊消息
     * @return 插入结果
     */
    Boolean insert(ImSingleMessagePo singleMessagePo);

    /**
     * 批量插入单聊消息
     *
     * @param singleMessagePoList 单聊消息列表
     * @return 批量插入结果
     */
    Boolean batchInsert(List<ImSingleMessagePo> singleMessagePoList);

    /**
     * 更新单聊消息
     *
     * @param singleMessagePo 单聊消息
     * @return 更新结果
     */
    Boolean update(ImSingleMessagePo singleMessagePo);

    /**
     * 删除单聊消息
     *
     * @param messageId 消息ID
     * @return 删除结果
     */
    Boolean deleteById(String messageId);

    /**
     * 查询单聊消息最后消息
     *
     * @param fromId 发送方ID
     * @param toId   接收方ID
     * @return 单聊消息
     */
    ImSingleMessagePo last(String fromId, String toId);

    /**
     * 查询单聊消息已读状态
     *
     * @param fromId 发送方ID
     * @param toId   接收方ID
     * @param code   状态码
     * @return 单聊消息已读状态
     */
    Integer selectReadStatus(String fromId, String toId, Integer code);

}
