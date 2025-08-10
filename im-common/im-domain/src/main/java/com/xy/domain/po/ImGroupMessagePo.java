package com.xy.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @TableName im_group_message
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "im_group_message")
public class ImGroupMessagePo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

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
    @TableField(value = "extra")
    private String extra;

    /**
     * 删除标识（1正常，0删除）
     */
    @TableLogic(value = "1", delval = "0")
    @TableField(value = "del_flag")
    private Integer delFlag;

    /**
     * 消息序列
     */
    @TableField(value = "sequence")
    private String sequence;

    /**
     * 随机标识
     */
    @TableField(value = "message_random")
    private String messageRandom;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private Long updateTime;

    @Version
    private Integer version;
}