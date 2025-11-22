package com.xy.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @TableName im_group_member
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "im_group_member")
public class ImGroupMemberPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 群成员ID
     */
    @TableId(value = "group_member_id")
    private String groupMemberId;
    /**
     * 群组ID
     */
    @TableField(value = "group_id")
    private String groupId;
    /**
     * 成员用户ID
     */
    @TableField(value = "member_id")
    private String memberId;
    /**
     * 群成员角色（0普通成员，1管理员，2群主）
     */
    @TableField(value = "role")
    private Integer role;
    /**
     * 最后发言时间
     */
    @TableField(value = "speak_date")
    private Long speakDate;
    /**
     * 是否禁言（0不禁言，1禁言）
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
    private Long joinTime;
    /**
     * 离开时间
     */
    @TableField(value = "leave_time")
    private Long leaveTime;
    /**
     * 加入类型
     */
    @TableField(value = "join_type")
    private String joinType;

    /**
     * 群备注
     */
    @TableField(value = "remark")
    private String remark;
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