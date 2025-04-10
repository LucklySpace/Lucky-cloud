package com.xy.auth.domain.dto;

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
public class ImUserDto implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId
    @TableField(value = "user_id")
    private String userId;
    /**
     *
     */
    @TableField(value = "user_name")
    private String username;
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
    private Long createTime;
    /**
     *
     */
    @TableField(value = "update_time")
    private Long updateTime;

}