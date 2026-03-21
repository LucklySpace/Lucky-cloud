package com.xy.lucky.message.service;


import com.xy.lucky.message.domain.dto.ChatDto;
import com.xy.lucky.core.model.*;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImSingleMessagePo;

import java.util.List;

/**
 * 消息业务服务，负责消息发送、撤回、确认以及历史消息查询。
 */
public interface MessageService {

    /**
     * 发送单聊消息。
     *
     * @param singleMessageDto 单聊消息内容
     * @return 回填消息 ID 和时间后的消息对象
     */
    IMSingleMessage sendSingleMessage(IMSingleMessage singleMessageDto);

    /**
     * 发送群聊消息。
     *
     * @param groupMessageDto 群聊消息内容
     * @return 回填消息 ID 和时间后的消息对象
     */
    IMGroupMessage sendGroupMessage(IMGroupMessage groupMessageDto);

    /**
     * 发送视频消息。
     *
     * @param videoMessageDto 视频消息内容
     */
    void sendVideoMessage(IMVideoMessage videoMessageDto);

    /**
     * 撤回已发送消息。
     *
     * @param dto 撤回动作参数
     */
    void recallMessage(IMessageAction dto);

    /**
     * 发送群组操作消息。
     *
     * @param groupActionDto 群组操作内容
     */
    void sendGroupAction(IMGroupAction groupActionDto);

    /**
     * 接收客户端 ACK，更新消息送达状态。
     *
     * @param messageId 消息 ID
     * @param userId    确认用户 ID
     */
    void acknowledge(String messageId, String userId);

    /**
     * 补发用户离线期间未收到的消息。
     *
     * @param userId 用户 ID
     */
    void replayOfflineMessages(String userId);

    /**
     * 查询单聊消息列表。
     *
     * @param chatDto 查询条件
     * @return 单聊消息列表
     */
    List<ImSingleMessagePo> singleList(ChatDto chatDto);

    /**
     * 查询群聊消息列表。
     *
     * @param chatDto 查询条件
     * @return 群聊消息列表
     */
    List<ImGroupMessagePo> groupList(ChatDto chatDto);
}
