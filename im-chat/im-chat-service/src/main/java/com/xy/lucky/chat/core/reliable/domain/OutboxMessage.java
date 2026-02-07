package com.xy.lucky.chat.core.reliable.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 发件箱消息实体
 */
@Data
@Accessors(chain = true)
public class OutboxMessage {

    /**
     * 消息 ID
     */
    private String messageId;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 消息内容
     */
    private String payload;

    /**
     * 消息状态
     */
    private OutboxStatus status;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 错误信息
     */
    private String errorMessage;
}

