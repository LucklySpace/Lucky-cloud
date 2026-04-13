package com.xy.lucky.message.message.outbox;

/**
 * Outbox 记录服务，负责消息投递状态在数据库中的流转。
 */
public interface OutboxRecordService {

    /**
     * 创建待发送的 Outbox 记录。
     *
     * @param messageId  业务消息 ID
     * @param payload    投递负载
     * @param exchange   目标交换机
     * @param routingKey 路由键
     * @return Outbox 主键 ID
     */
    Long createPending(String messageId, String payload, String exchange, String routingKey);

    /**
     * 标记 Outbox 已发送到 Broker。
     *
     * @param outboxId  Outbox 主键 ID
     * @param attempts  已尝试次数
     */
    void markSent(Long outboxId, int attempts);

    /**
     * 标记 Outbox 进入待重试状态。
     *
     * @param outboxId   Outbox 主键 ID
     * @param attempts   已尝试次数
     * @param nextTryAt  下次重试时间戳
     * @param reason     失败原因
     */
    void markPendingForRetry(Long outboxId, int attempts, long nextTryAt, String reason);

    /**
     * 标记 Outbox 进入死信状态。
     *
     * @param outboxId  Outbox 主键 ID
     * @param attempts  已尝试次数
     * @param reason    死信原因
     */
    void markDlx(Long outboxId, int attempts, String reason);

    /**
     * 按业务消息 ID 标记 Outbox 已被客户端确认。
     *
     * @param messageId 业务消息 ID
     */
    boolean markDeliveredByMessageId(String messageId);
}
