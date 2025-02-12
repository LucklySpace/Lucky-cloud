package com.xy.server.domain.po;

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
 * @TableName im_group
 */
@TableName(value = "im_group")
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ImGroupPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * groupId
     */
    @TableId(value = "group_id", type = IdType.AUTO)
    private String groupId;
    /**
     * 群主
     */
    @TableField(value = "owner_id")
    private String ownerId;
    /**
     * 群类型 1私有群（类似微信） 2公开群(类似qq）
     */
    @TableField(value = "group_type")
    private Integer groupType;
    /**
     *
     */
    @TableField(value = "group_name")
    private String groupName;
    /**
     * 是否全员禁言，0 不禁言；1 全员禁言
     */
    @TableField(value = "mute")
    private Integer mute;
    /**
     * 申请加群选项包括如下几种：
     * 0 表示禁止任何人申请加入
     * 1 表示需要群主或管理员审批
     * 2 表示允许无需审批自由加入群组
     */
    @TableField(value = "apply_join_type")
    private Integer applyJoinType;
    /**
     *
     */
    @TableField(value = "avatar")
    private String avatar;
    /**
     *
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
     * 群状态 0正常 1解散
     */
    @TableField(value = "status")
    private Integer status;
    /**
     *
     */
    @TableField(value = "sequence")
    private Long sequence;
    /**
     *
     */
    @TableField(value = "create_time")
    private Long createTime;
    /**
     * 来源
     */
    @TableField(value = "extra")
    private String extra;
    /**
     *
     */
    @TableField(value = "update_time")
    private Long updateTime;

}