package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @TableName im_group_message_status
 */
@TableName(value = "im_group_message_status")
@Data
@Accessors(chain = true)
public class ImGroupMessageStatusPo implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableField(value = "group_id")
    private String groupId;
    /**
     *
     */
    @TableField(value = "message_id")
    private String messageId;
    /**
     *
     */
    @TableField(value = "to_id")
    private String toId;
    /**
     *
     */
    @TableField(value = "read_status")
    private Integer readStatus;

}