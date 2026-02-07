package com.xy.lucky.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 授权码响应")
public class OAuth2AuthorizeResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "授权码")
    private String code;

    @Schema(description = "透传的 state")
    private String state;

    @Schema(description = "重定向地址")
    private String redirectUri;

    @Schema(description = "过期时间(秒)")
    private long expiresIn;
}

