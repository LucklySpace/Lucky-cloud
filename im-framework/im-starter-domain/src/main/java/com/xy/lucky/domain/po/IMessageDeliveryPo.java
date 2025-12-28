package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * 消息投递状态表
 * 记录每个 message 到每个用户的投递状态
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息投递状态记录")
@TableName(value = "im_message_delivery")
public class IMessageDeliveryPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 消息ID
     */
    @TableField(value = "message_id")
    private String messageId;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * im-connect实例ID
     */
    @TableField(value = "broker_id")
    private String brokerId;

    /**
     * 状态 (PENDING / SENT / DELIVERED / FAILED)
     */
    @TableField(value = "status")
    private String status;

    /**
     * 尝试次数
     */
    @TableField(value = "attempts")
    private Integer attempts;

    /**
     * 最后尝试时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "last_attempt_at")
    private Long lastAttemptAt;

    /**
     * 投递时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "delivered_at")
    private Long deliveredAt;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Long createdAt;
}
