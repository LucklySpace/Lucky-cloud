package com.xy.lucky.rpc.api.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 短链重定向请求 DTO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ShortLinkRedirectDto", description = "短链重定向请求")
public class ShortLinkRedirectDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "短码", requiredMode = Schema.RequiredMode.REQUIRED, example = "a1B2c3")
    @NotBlank(message = "shortCode 不能为空")
    @Size(max = 64, message = "shortCode 最长 64 字符")
    private String shortCode;
}
