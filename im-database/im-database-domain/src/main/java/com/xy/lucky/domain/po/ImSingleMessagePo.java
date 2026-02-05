package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.xy.lucky.domain.BasePo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @TableName im_srivate_message
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单聊消息")
@TableName(value = "im_single_message")
public class ImSingleMessagePo extends BasePo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 消息ID
     */
    @TableId(value = "message_id")
    private String messageId;
    /**
     * 发送者用户ID
     */
    @TableField(value = "from_id")
    private String fromId;
    /**
     * 接收者用户ID
     */
    @TableField(value = "to_id")
    private String toId;
    /**
     * 消息内容
     */
    @TableField(value = "message_body", typeHandler = JacksonTypeHandler.class)
    private Object messageBody;
    /**
     * 发送时间
     */
    @TableField(value = "message_time")
    private Long messageTime;
    /**
     * 消息类型
     */
    @TableField(value = "message_content_type")
    private Integer messageContentType;
    /**
     * 阅读状态（1已读）
     */
    @TableField(value = "read_status")
    private Integer readStatus;
    /**
     * 扩展字段
     */
    @TableField(value = "extra", typeHandler = JacksonTypeHandler.class)
    private Object extra;

    /**
     * 消息序列
     */
    @TableField(value = "sequence")
    private Long sequence;
    /**
     * 随机标识
     */
    @TableField(value = "message_random")
    private String messageRandom;
}
