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
@Table("im_friendship_request")
@Schema(description = "好友请求表")
public class ImFriendshipRequestEntity {

    @Id
    @Schema(description = "请求ID")
    @Column("id")
    private String id;

    @Schema(description = "请求发起者")
    @Column("from_id")
    private String fromId;

    @Schema(description = "请求接收者")
    @Column("to_id")
    private String toId;

    @Schema(description = "备注")
    @Column("remark")
    private String remark;

    @Schema(description = "是否已读（1已读）")
    @Column("read_status")
    private Integer readStatus;

    @Schema(description = "好友来源")
    @Column("add_source")
    private String addSource;

    @Schema(description = "好友验证信息")
    @Column("message")
    private String message;

    @Schema(description = "审批状态（0未审批，1同意，2拒绝）")
    @Column("approve_status")
    private Integer approveStatus;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "序列号")
    @Column("sequence")
    private Long sequence;

    @Schema(description = "删除标识（1正常，0删除）")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "版本信息")
    @Column("version")
    private Integer version;
}
