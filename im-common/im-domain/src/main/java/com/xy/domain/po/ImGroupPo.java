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
 * @TableName im_group
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "im_group")
public class ImGroupPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 群组ID
     */
    @TableId(value = "group_id")
    private String groupId;
    /**
     * 群主用户ID
     */
    @TableField(value = "owner_id")
    private String ownerId;
    /**
     * 群类型（1私有群，2公开群）
     */
    @TableField(value = "group_type")
    private Integer groupType;
    /**
     * 群名称
     */
    @TableField(value = "group_name")
    private String groupName;
    /**
     * 是否全员禁言（0不禁言，1禁言）
     */
    @TableField(value = "mute")
    private Integer mute;
    /**
     * 申请加群方式（0禁止申请，1需要审批，2允许自由加入）
     */
    @TableField(value = "apply_join_type")
    private Integer applyJoinType;
    /**
     * 群头像
     */
    @TableField(value = "avatar")
    private String avatar;
    /**
     * 最大成员数
     */
    @TableField(value = "max_member_count")
    private Integer maxMemberCount;
    /**
     * 群简介
     */
    @TableField(value = "introduction")
    private String introduction;
    /**
     * 群公告
     */
    @TableField(value = "notification")
    private String notification;
    /**
     * 群状态（0正常，1解散）
     */
    @TableField(value = "status")
    private Integer status;
    /**
     * 消息序列号
     */
    @TableField(value = "sequence")
    private Long sequence;
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
     * 扩展字段
     */
    @TableField(value = "extra")
    private String extra;

    @Version
    private Integer version;

    @TableLogic(value = "0", delval = "1")
    @TableField(value = "del_flag")
    private Integer delFlag;
}