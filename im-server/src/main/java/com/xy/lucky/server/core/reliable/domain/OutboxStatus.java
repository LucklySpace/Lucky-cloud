package com.xy.lucky.server.core.reliable.domain;

/**
 * 发件箱消息状态
 */
public enum OutboxStatus {

    /**
     * 待发送
     */
    PENDING(0),

    /**
     * 已确认（成功）
     */
    CONFIRMED(1),

    /**
     * 发送失败
     */
    FAILED(2),

    /**
     * 消息被退回（路由失败）
     */
    RETURNED(3),

    /**
     * 重试中
     */
    RETRY(4),

    /**
     * 死信（超过重试次数）
     */
    DEAD(5);

    private final int code;

    OutboxStatus(int code) {
        this.code = code;
    }

    public static OutboxStatus fromCode(int code) {
        for (OutboxStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }

    public int getCode() {
        return code;
    }
}

