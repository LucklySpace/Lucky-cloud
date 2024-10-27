package com.xy.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName im_friendship_group
 */
@TableName(value = "im_friendship_group")
@Data
public class ImFriendshipGroup implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "group_id", type = IdType.AUTO)
    private Integer group_id;
    /**
     * from_id
     */
    @TableField(value = "from_id")
    private String from_id;
    /**
     *
     */
    @TableField(value = "group_name")
    private String group_name;
    /**
     *
     */
    @TableField(value = "sequence")
    private Long sequence;
    /**
     *
     */
    @TableField(value = "create_time")
    private Long create_time;
    /**
     *
     */
    @TableField(value = "update_time")
    private Long update_time;
    /**
     *
     */
    @TableField(value = "del_flag")
    private Integer del_flag;

}