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
@Table("im_friendship")
@Schema(description = "好友关系实体")
public class ImFriendshipEntity {

    @Id
    @Schema(description = "所有者ID")
    @Column("owner_id")
    private String ownerId;

    @Schema(description = "对端ID")
    @Column("to_id")
    private String toId;

    @Schema(description = "备注")
    @Column("remark")
    private String remark;

    @Schema(description = "删除标志")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "黑名单标志")
    @Column("black")
    private Integer black;

    @Schema(description = "序列")
    @Column("sequence")
    private Long sequence;

    @Schema(description = "黑名单序列")
    @Column("black_sequence")
    private Long blackSequence;

    @Schema(description = "添加来源")
    @Column("add_source")
    private String addSource;

    @Schema(description = "扩展字段")
    @Column("extra")
    private String extra;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "版本号")
    @Column("version")
    private Integer version;
}
