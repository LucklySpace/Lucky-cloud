package com.xy.lucky.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "刷新令牌元数据")
public class AuthRefreshToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(description = "设备唯一标识")
    private String deviceId;

    @Schema(description = "签发时间戳")
    private long issuedAt;

    @Schema(description = "令牌版本号，用于踢人和会话失效控制")
    private long tokenVersion;
}
