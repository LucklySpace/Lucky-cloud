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
    private String group_id;
    /**
     *
     */
    @TableField(value = "message_id")
    private String message_id;
    /**
     *
     */
    @TableField(value = "to_id")
    private String to_id;
    /**
     *
     */
    @TableField(value = "read_status")
    private Integer read_status;

}