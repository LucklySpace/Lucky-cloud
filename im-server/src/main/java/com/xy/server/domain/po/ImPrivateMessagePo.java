package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName im_private_message
 */
@TableName(value = "im_private_message")
@Data
public class ImPrivateMessagePo implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "message_id")
    private String messageId;
    /**
     *
     */
    @TableField(value = "from_id")
    private String fromId;
    /**
     *
     */
    @TableField(value = "to_id")
    private String toId;
    /**
     *
     */
    @TableField(value = "message_body", typeHandler = JacksonTypeHandler.class)
    private Object messageBody;
    /**
     *
     */
    @TableField(value = "message_time")
    private Long messageTime;
    /**
     *
     */
    @TableField(value = "message_content_type")
    private String messageContentType;
    /**
     *
     */
    @TableField(value = "read_status")
    private Integer readStatus;
    /**
     *
     */
    @TableField(value = "extra")
    private String extra;
    /**
     *
     */
    @TableField(value = "del_flag")
    private Integer delFlag;
    /**
     *
     */
    @TableField(value = "sequence")
    private Long sequence;
    /**
     *
     */
    @TableField(value = "message_random")
    private String messageRandom;
    /**
     *
     */
    @TableField(value = "create_time")
    private Long createTime;


}