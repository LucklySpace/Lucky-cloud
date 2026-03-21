package com.xy.lucky.message.message.outbox;

// 消息事件
public enum OutboxEvent {
    // 创建
    CREATE,
    // 响应
    BROKER_ACK,
    // 拒绝
    BROKER_NACK,
    // 重试超时
    RETRY_EXHAUSTED,
    // 客户端确认
    CLIENT_ACK
}
