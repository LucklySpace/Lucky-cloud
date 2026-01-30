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
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "二维码扫码状态")
public class QRCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "二维码code")
    private String code;

    @Schema(description = "二维码状态")
    private String status;

    @Schema(description = "创建时间戳")
    private Long createdAt;

    @Schema(description = "扫描时间戳")
    private Long scannedAt;

    @Schema(description = "登录时间戳")
    private long loggedInAt;

    @Schema(description = "临时登录密码")
    private String password;
}
