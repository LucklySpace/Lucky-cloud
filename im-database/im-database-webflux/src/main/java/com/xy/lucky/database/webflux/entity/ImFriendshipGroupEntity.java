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
@Table("im_friendship_group")

@Schema(description = "好友分组实体")

public class ImFriendshipGroupEntity {

    @Schema(description = "所有者ID")
    @Column("from_id")
    private String fromId;

    @Id
    @Schema(description = "分组ID")
    @Column("group_id")
    private String groupId;

    @Schema(description = "分组名称")
    @Column("group_name")
    private String groupName;

    @Schema(description = "序列")
    @Column("sequence")
    private Long sequence;

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
