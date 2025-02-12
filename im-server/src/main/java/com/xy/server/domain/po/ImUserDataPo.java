package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName im_user_data
 */
@TableName(value = "im_user_data")
@Data
public class ImUserDataPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "user_id")
    private String userId;
    /**
     * 昵称
     */
    @TableField(value = "name")
    private String name;
    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;
    /**
     * 头像
     */
    @TableField(value = "avatar")
    private String avatar;
    /**
     * 性别
     */
    @TableField(value = "gender")
    private Integer gender;
    /**
     * 生日
     */
    @TableField(value = "birthday")
    private String birthDay;
    /**
     * 地址
     */
    @TableField(value = "location")
    private String location;
    /**
     * 个性签名
     */
    @TableField(value = "self_signature")
    private String selfSignature;
    /**
     * 加好友验证类型（Friend_AllowType） 1无需验证 2需要验证
     */
    @TableField(value = "friend_allow_type")
    private Integer friendAllowType;
    /**
     * 禁用标识 1禁用
     */
    @TableField(value = "forbidden_flag")
    private Integer forbiddenFlag;
    /**
     * 管理员禁止用户添加加好友：0 未禁用 1 已禁用
     */
    @TableField(value = "disable_add_friend")
    private Integer disableAddFriend;
    /**
     * 禁言标识 1禁言
     */
    @TableField(value = "silent_flag")
    private Integer silentFlag;
    /**
     * 用户类型 1普通用户 2客服 3机器人
     */
    @TableField(value = "user_type")
    private Integer userType;
    /**
     *
     */
    @TableField(value = "del_flag")
    private Integer delFlag;
    /**
     *
     */
    @TableField(value = "extra")
    private String extra;


}