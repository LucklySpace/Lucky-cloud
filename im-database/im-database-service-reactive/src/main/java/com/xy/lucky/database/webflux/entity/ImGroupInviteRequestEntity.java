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
@Table("im_group_invite_request")
@Schema(description = "群邀请请求表")
public class ImGroupInviteRequestEntity {

    @Id
    @Schema(description = "邀请请求ID")
    @Column("request_id")
    private String requestId;

    @Schema(description = "群组ID")
    @Column("group_id")
    private String groupId;

    @Schema(description = "邀请发起者用户ID")
    @Column("from_id")
    private String fromId;

    @Schema(description = "被邀请者用户ID")
    @Column("to_id")
    private String toId;

    @Schema(description = "验证者用户ID")
    @Column("verifier_id")
    private String verifierId;

    @Schema(description = "群主或管理员验证状态")
    @Column("verifier_status")
    private Integer verifierStatus;

    @Schema(description = "邀请验证信息")
    @Column("message")
    private String message;

    @Schema(description = "被邀请人状态")
    @Column("approve_status")
    private Integer approveStatus;

    @Schema(description = "邀请来源")
    @Column("add_source")
    private Integer addSource;

    @Schema(description = "邀请过期时间")
    @Column("expire_time")
    private Long expireTime;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "删除标识")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "版本信息")
    @Column("version")
    private Integer version;
}
