package com.xy.auth.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.xy.core.constants.IMConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 登录响应
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class IMLoginResult {

    @Schema(description = "accessToken")
    private String accessToken;

    @Schema(description = "userId")
    private String userId;

    @Schema(description = "token 类型", example = "Bearer")
    private String tokenType = IMConstant.BEARER_PREFIX;

    @Schema(description = "过期时间(单位：秒)", example = "604800")
    private Integer expiration;

    @Schema(description = "refreshToken")
    private String refreshToken;

    @Schema(description = "im-connect endpoints")
    private List<IMConnectEndpointMetadata> connectEndpoints;

}