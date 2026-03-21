package com.xy.lucky.message.message.dispatch;

import lombok.Builder;

import java.util.List;

/**
 * 单次消息分发任务，按 broker 粒度封装发送上下文。
 *
 * @param correlationId RabbitMQ 关联 ID
 * @param messageId     业务消息 ID
 * @param brokerId      目标 broker 路由键
 * @param userIds       当前任务对应的目标用户
 * @param payload       序列化后的消息内容
 * @param attempt       当前重试次数
 */
@Builder(toBuilder = true)
public record MessageDispatchTask(
        String correlationId,
        String messageId,
        Long outboxId,
        String brokerId,
        List<String> userIds,
        String payload,
        int attempt,
        long firstEnqueueAt
) {
}
