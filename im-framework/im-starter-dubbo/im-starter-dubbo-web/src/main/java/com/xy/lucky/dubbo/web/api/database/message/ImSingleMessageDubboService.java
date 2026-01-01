package com.xy.lucky.dubbo.web.api.database.message;

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
    List<ImSingleMessagePo> queryList(String userId, Long sequence);

    /**
     * 查询单聊消息
     *
     * @param messageId 消息ID
     * @return 单聊消息
     */
    ImSingleMessagePo queryOne(String messageId);

    /**
     * 插入单聊消息
     *
     * @param singleMessagePo 单聊消息
     * @return 插入结果
     */
    Boolean creat(ImSingleMessagePo singleMessagePo);

    /**
     * 批量插入单聊消息
     *
     * @param singleMessagePoList 单聊消息列表
     * @return 批量插入结果
     */
    Boolean creatBatch(List<ImSingleMessagePo> singleMessagePoList);

    /**
     * 更新单聊消息
     *
     * @param singleMessagePo 单聊消息
     * @return 更新结果
     */
    Boolean modify(ImSingleMessagePo singleMessagePo);

    /**
     * 删除单聊消息
     *
     * @param messageId 消息ID
     * @return 删除结果
     */
    Boolean removeOne(String messageId);

    /**
     * 查询单聊消息最后消息
     *
     * @param fromId 发送方ID
     * @param toId   接收方ID
     * @return 单聊消息
     */
    ImSingleMessagePo queryLast(String fromId, String toId);

    /**
     * 查询单聊消息已读状态
     *
     * @param fromId 发送方ID
     * @param toId   接收方ID
     * @param code   状态码
     * @return 单聊消息已读状态
     */
    Integer queryReadStatus(String fromId, String toId, Integer code);

}
