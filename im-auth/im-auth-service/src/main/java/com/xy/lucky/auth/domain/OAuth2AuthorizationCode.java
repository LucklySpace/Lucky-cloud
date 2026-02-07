package com.xy.lucky.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 授权码元数据")
public class OAuth2AuthorizationCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "授权码")
    private String code;

    @Schema(description = "客户端 ID")
    private String clientId;

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(description = "重定向回调地址")
    private String redirectUri;

    @Schema(description = "PKCE 挑战码")
    private String codeChallenge;

    @Schema(description = "PKCE 挑战方法")
    private String codeChallengeMethod;

    @Schema(description = "授权范围列表")
    private List<String> scopes;

    @Schema(description = "过期时间戳")
    private long expiresAt;

    @Schema(description = "签发时间戳")
    private long issuedAt;
}

