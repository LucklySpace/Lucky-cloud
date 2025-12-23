package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链响应
 */
@Data
@Schema(name = "ShortLinkVo", description = "短链信息响应")
public class ShortLinkVo {

    @Schema(description = "原始URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/docs/123")
    @NotBlank(message = "originalUrl 不能为空")
    @Size(max = 2048, message = "originalUrl 最长 2048 字符")
    private String originalUrl;

    @Schema(description = "可选：过期时间（秒），默认不设置表示永久有效", example = "86400")
    private Long expireSeconds;

    @Schema(description = "短码", example = "a1B2c3")
    private String shortCode;

    @Schema(description = "完整短链", example = "https://s.example.com/api/v1/short/r/a1B2c3")
    private String shortUrl;

    @Schema(description = "访问次数", example = "10")
    private Integer visitCount;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
