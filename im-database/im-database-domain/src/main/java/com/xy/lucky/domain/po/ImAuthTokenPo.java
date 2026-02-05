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
@Schema(description = "认证令牌持久化信息")
@TableName(value = "im_auth_token")
public class ImAuthTokenPo extends BasePo {

    @TableId(value = "id")
    private String id;

    @Schema(description = "用户id")
    @TableField(value = "user_id")
    private String userId;

    @Schema(description = "设备id")
    @TableField(value = "device_id")
    private String deviceId;

    @Schema(description = "客户端ip")
    @TableField(value = "client_ip")
    private String clientIp;

    @Schema(description = "用户代理")
    @TableField(value = "user_agent")
    private String userAgent;

    @Schema(description = "访问令牌")
    @TableField(value = "access_token_hash")
    private String accessTokenHash;

    @Schema(description = "刷新令牌")
    @TableField(value = "refresh_token_hash")
    private String refreshTokenHash;

    @Schema(description = "令牌版本")
    @TableField(value = "token_version")
    private Long tokenVersion;

    @Schema(description = "令牌族")
    @TableField(value = "token_family_id")
    private String tokenFamilyId;

    @Schema(description = "令牌序列号")
    @TableField(value = "sequence_number")
    private Integer sequenceNumber;

    @Schema(description = "令牌颁发时间")
    @TableField(value = "issued_at")
    private Long issuedAt;

    @Schema(description = "令牌访问到期时间")
    @TableField(value = "access_expires_at")
    private Long accessExpiresAt;

    @Schema(description = "令牌绝对到期时间")
    @TableField(value = "absolute_expires_at")
    private Long absoluteExpiresAt;

    @Schema(description = "令牌是否被使用")
    @TableField(value = "used")
    private Integer used;

    @Schema(description = "令牌是否被撤销")
    @TableField(value = "revoked_at")
    private Long revokedAt;

    @Schema(description = "撤销原因")
    @TableField(value = "revoke_reason")
    private String revokeReason;

    @Schema(description = "授权类型")
    @TableField(value = "grant_type")
    private String grantType;

    @Schema(description = "授权范围")
    @TableField(value = "scope")
    private String scope;
}
