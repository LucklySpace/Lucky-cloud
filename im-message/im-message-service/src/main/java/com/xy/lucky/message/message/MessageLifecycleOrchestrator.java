package com.xy.lucky.message.message;

import java.util.Collection;

/**
 * 消息生命周期编排接口，负责统一组织消息从分发到状态更新的全链路流程。
 */
public interface MessageLifecycleOrchestrator {

    /**
     * 分发消息到目标用户，在线用户实时推送，离线用户写入离线队列。
     *
     * @param messageType   消息类型编码
     * @param payload       原始业务消息体
     * @param targetUserIds 目标用户集合
     * @param messageId     全局消息 ID
     */
    void dispatch(Integer messageType, Object payload, Collection<String> targetUserIds, String messageId);

    /**
     * 同步客户端确认状态。
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     */
    void acknowledge(String messageId, String userId);

    /**
     * 将用户离线消息重放到实时通道。
     *
     * @param userId 用户 ID
     */
    void replayOfflineMessages(String userId);
}
