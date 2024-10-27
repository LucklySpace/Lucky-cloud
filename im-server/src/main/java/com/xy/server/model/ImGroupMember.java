package com.xy.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @TableName im_group_member
 */
@TableName(value = "im_group_member")
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ImGroupMember implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "group_member_id", type = IdType.AUTO)
    private Long group_member_id;
    /**
     * group_id
     */
    @TableField(value = "group_id")
    private String group_id;
    /**
     * 成员id
     */
    @TableField(value = "member_id")
    private String member_id;
    /**
     * 群成员类型，0 普通成员, 1 管理员, 2 群主， 3 禁言，4 已经移除的成员
     */
    @TableField(value = "role")
    private Integer role;
    /**
     *
     */
    @TableField(value = "speak_date")
    private Long speak_date;
    /**
     * 是否全员禁言，0 不禁言；1 全员禁言
     */
    @TableField(value = "mute")
    private Integer mute;
    /**
     * 群昵称
     */
    @TableField(value = "alias")
    private String alias;
    /**
     * 加入时间
     */
    @TableField(value = "join_time")
    private Long join_time;
    /**
     * 离开时间
     */
    @TableField(value = "leave_time")
    private Long leave_time;
    /**
     * 加入类型
     */
    @TableField(value = "join_type")
    private String join_type;
    /**
     *
     */
    @TableField(value = "extra")
    private String extra;

}