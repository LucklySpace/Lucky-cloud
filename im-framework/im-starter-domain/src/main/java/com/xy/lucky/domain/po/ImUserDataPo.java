package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @TableName im_user_data
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户扩展资料信息")
@TableName(value = "im_user_data")
public class ImUserDataPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "user_id")
    private String userId;

    /**
     * 昵称
     */
    @TableField(value = "name")
    private String name;

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
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "birthday")
    private String birthday;

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
     * 加好友验证类型（1无需验证，2需要验证）
     */
    @TableField(value = "friend_allow_type")
    private Integer friendAllowType;

    /**
     * 禁用标识（1禁用）
     */
    @TableField(value = "forbidden_flag")
    private Integer forbiddenFlag;

    /**
     * 管理员禁止添加好友：0未禁用，1已禁用
     */
    @TableField(value = "disable_add_friend")
    private Integer disableAddFriend;

    /**
     * 禁言标识（1禁言）
     */
    @TableField(value = "silent_flag")
    private Integer silentFlag;

    /**
     * 用户类型（1普通用户，2客服，3机器人）
     */
    @TableField(value = "user_type")
    private Integer userType;

    /**
     * 扩展字段
     */
    @TableField(value = "extra")
    private String extra;

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

    /**
     * 删除标识（1正常，0删除）
     */
    @TableLogic(value = "1", delval = "0")
    @TableField(value = "del_flag")
    private Integer delFlag;

    @Version
    private Integer version;
}
