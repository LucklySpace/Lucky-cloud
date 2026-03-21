package com.xy.lucky.message.message.outbox;

// 待处理，已发送，已送达，发送失败，死信
public enum OutboxStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    DLX;

    public String value() {
        return name();
    }

    public static OutboxStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OutboxStatus.valueOf(value.trim().toUpperCase());
    }
}
