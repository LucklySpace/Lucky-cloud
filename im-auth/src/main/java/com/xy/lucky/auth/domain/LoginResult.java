package com.xy.lucky.auth.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.xy.lucky.core.constants.IMConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 登录响应
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户登录成功响应")
public class LoginResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "accessToken")
    private String accessToken;

    @Schema(description = "refreshToken")
    private String refreshToken;

    @Schema(description = "userId")
    private String userId;

    @Schema(description = "token 类型", example = "Bearer")
    private String tokenType = IMConstant.BEARER_PREFIX;

    @Schema(description = "accessToken  过期时间(单位：秒)", example = "604800")
    private Integer accessExpiration;

    @Schema(description = "refreshToken 过期时间(单位：秒)", example = "604800")
    private Integer refreshExpiration;

    @Schema(description = "token版本")
    private String version;

    @Schema(description = "im-connect endpoints")
    private List<ConnectEndpointMetadata> connectEndpoints = Collections.emptyList();

}