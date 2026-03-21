package com.xy.lucky.message.message.offline;

import lombok.Builder;

/**
 * 离线消息记录，描述单条离线待补发数据。
 *
 * @param messageId   业务消息 ID
 * @param messageType 消息类型编码
 * @param payload     序列化后的消息包体
 */
@Builder(toBuilder = true)
public record OfflineMessageRecord(
        String messageId,
        Integer messageType,
        String payload
) {
}
