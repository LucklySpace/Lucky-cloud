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

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群聊消息")
@TableName(value = "im_group_message")
public class ImGroupMessagePo extends BasePo {

    /**
     * 消息ID
     */
    @TableId(value = "message_id")
    private String messageId;

    /**
     * 群组ID
     */
    @TableField(value = "group_id")
    private String groupId;

    /**
     * 发送者用户ID
     */
    @TableField(value = "from_id")
    private String fromId;

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
     * 扩展字段
     */
    @TableField(value = "extra", typeHandler = JacksonTypeHandler.class)
    private Object extra;

    /**
     * 阅读状态（1已读）
     */
    @TableField(exist = false)
    private Integer readStatus;

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
