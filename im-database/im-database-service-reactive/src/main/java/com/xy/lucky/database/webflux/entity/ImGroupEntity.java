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
@Table("im_group")
@Schema(description = "群组实体")
public class ImGroupEntity {

    @Id
    @Schema(description = "群ID")
    @Column("group_id")
    private String groupId;

    @Schema(description = "群主ID")
    @Column("owner_id")
    private String ownerId;

    @Schema(description = "群类型")
    @Column("group_type")
    private Integer groupType;

    @Schema(description = "群名称")
    @Column("group_name")
    private String groupName;

    @Schema(description = "是否全员禁言")
    @Column("mute")
    private Integer mute;

    @Schema(description = "加群方式")
    @Column("apply_join_type")
    private Integer applyJoinType;

    @Schema(description = "群头像")
    @Column("avatar")
    private String avatar;

    @Schema(description = "最大成员数")
    @Column("max_member_count")
    private Integer maxMemberCount;

    @Schema(description = "群简介")
    @Column("introduction")
    private String introduction;

    @Schema(description = "群公告")
    @Column("notification")
    private String notification;

    @Schema(description = "群状态")
    @Column("status")
    private Integer status;

    @Schema(description = "序列")
    @Column("sequence")
    private Long sequence;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "扩展字段")
    @Column("extra")
    private String extra;

    @Schema(description = "版本号")
    @Column("version")
    private Integer version;

    @Schema(description = "删除标志")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "成员数")
    @Column("member_count")
    private Integer memberCount;
}
