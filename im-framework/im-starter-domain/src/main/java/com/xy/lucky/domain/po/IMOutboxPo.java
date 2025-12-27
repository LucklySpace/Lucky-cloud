package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * 消息投递表
 * 存储要发送到 MQ 的消息
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息投递 Outbox 实体")
@TableName(value = "im_outbox")
public class IMOutboxPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    /**
     * 业务 messageId
     */
    @TableField(value = "message_id")
    private String messageId;

    /**
     * 要发送的 JSON 负载（建议尽量轻量：可仅包含 messageId + 必要路由信息）
     */
    @TableField(value = "payload", typeHandler = JacksonTypeHandler.class)
    private Object payload;

    /**
     * 交换机名称
     */
    @TableField(value = "exchange")
    private String exchange;

    /**
     * 路由键
     */
    @TableField(value = "routing_key")
    private String routingKey;

    /**
     * 累积投递次数
     */
    @TableField(value = "attempts")
    private Integer attempts;

    /**
     * 投递状态：PENDING(待投递) / SENT(已确认) / FAILED(失败，需要人工介入) / DLX(死信)
     */
    @TableField(value = "status")
    private String status;

    /**
     * 最后错误信息
     */
    @TableField(value = "last_error")
    private String lastError;

    /**
     * 下一次重试时间（用以调度延迟重试）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "next_try_at", fill = FieldFill.INSERT)
    private Long nextTryAt;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Long createdAt;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private Long updatedAt;
}
