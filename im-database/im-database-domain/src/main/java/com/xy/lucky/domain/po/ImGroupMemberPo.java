package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.lucky.domain.BasePo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群成员信息")
@TableName(value = "im_group_member")
public class ImGroupMemberPo extends BasePo {

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
     * 禁言开始时间（毫秒时间戳）
     */
    @TableField(value = "mute_start_time")
    private Long muteStartTime;
    /**
     * 禁言结束时间（毫秒时间戳，null 表示永久禁言）
     */
    @TableField(value = "mute_end_time")
    private Long muteEndTime;
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

}
