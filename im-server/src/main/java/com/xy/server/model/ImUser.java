package com.xy.server.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName im_user
 */
@TableName(value = "im_user")
@Data
public class ImUser implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId
    @TableField(value = "user_id")
    private String user_id;
    /**
     *
     */
    @TableField(value = "user_name")
    private String user_name;
    /**
     *
     */
    @TableField(value = "password")
    private String password;
    /**
     *
     */
    @TableField(value = "mobile")
    private String mobile;
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

}