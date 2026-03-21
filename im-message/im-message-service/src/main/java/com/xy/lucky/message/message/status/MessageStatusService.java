package com.xy.lucky.message.message.status;

import java.util.Collection;

/**
 * 消息状态服务，负责消息状态同步、投递确认与失败标记。
 */
public interface MessageStatusService {

    /**
     * 初始化消息状态为待投递。
     *
     * @param messageId 消息 ID
     * @param userIds   目标用户列表
     */
    void markPending(String messageId, Collection<String> userIds);

    /**
     * 标记消息投递成功。
     *
     * @param messageId 消息 ID
     * @param userIds   成功用户列表
     */
    void markDelivered(String messageId, Collection<String> userIds);

    /**
     * 标记消息投递失败。
     *
     * @param messageId 消息 ID
     * @param userIds   失败用户列表
     * @param reason    失败原因
     */
    void markFailed(String messageId, Collection<String> userIds, String reason);

    /**
     * 记录客户端 ACK。
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     */
    void acknowledge(String messageId, String userId);
}
