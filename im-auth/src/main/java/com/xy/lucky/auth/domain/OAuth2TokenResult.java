package com.xy.lucky.auth.domain;

import com.xy.lucky.core.constants.IMConstant;
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
@Schema(description = "OAuth2 令牌响应")
public class OAuth2TokenResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer ")
    private String tokenType = IMConstant.BEARER_PREFIX;

    @Schema(description = "过期时间(秒)")
    private long expiresIn;

    @Schema(description = "授权范围")
    private List<String> scope;
}

