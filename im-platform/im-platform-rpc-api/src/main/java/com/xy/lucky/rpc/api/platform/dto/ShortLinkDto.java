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
 * 短链创建请求 DTO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ShortLinkDto", description = "短链创建请求")
public class ShortLinkDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原始URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/docs/123")
    @NotBlank(message = "originalUrl 不能为空")
    @Size(max = 2048, message = "originalUrl 最长 2048 字符")
    private String originalUrl;

    @Schema(description = "可选：过期时间（秒），默认不设置表示永久有效", example = "86400")
    private Long expireSeconds;
}
