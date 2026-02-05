package com.xy.lucky.database.webflux.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("im_group_member")
@Schema(description = "群成员实体")
public class ImGroupMemberEntity {

    @Id
    @Schema(description = "群成员关系ID")
    @Column("group_member_id")
    private String groupMemberId;

    @Schema(description = "群ID")

    @Column("group_id")
    private String groupId;

    @Schema(description = "成员ID")

    @Column("member_id")
    private String memberId;

    @Schema(description = "角色")

    @Column("role")
    private Integer role;

    @Schema(description = "可发言时间")

    @Column("speak_date")
    private Long speakDate;

    @Schema(description = "是否禁言")

    @Column("mute")
    private Integer mute;

    @Schema(description = "群内昵称")

    @Column("alias")
    private String alias;

    @Schema(description = "加入时间")

    @Column("join_time")
    private Long joinTime;

    @Schema(description = "离开时间")

    @Column("leave_time")
    private Long leaveTime;

    @Schema(description = "加入方式")

    @Column("join_type")
    private String joinType;

    @Schema(description = "备注")

    @Column("remark")
    private String remark;

    @Schema(description = "扩展字段")

    @Column("extra")
    private String extra;

    @Schema(description = "创建时间")

    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")

    @Column("update_time")
    private Long updateTime;

    @Schema(description = "删除标志")

    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "版本号")

    @Column("version")
    private Integer version;
}
