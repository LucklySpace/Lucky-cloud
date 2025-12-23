package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链响应
 */
@Data
@Schema(name = "ShortLinkVo", description = "短链信息响应")
public class ShortLinkVo {

    @Schema(description = "短码", example = "a1B2c3")
    private String shortCode;

    @Schema(description = "完整短链", example = "https://s.example.com/api/v1/short/r/a1B2c3")
    private String shortUrl;

    @Schema(description = "原始URL", example = "https://example.com/docs/123")
    private String originalUrl;

    @Schema(description = "访问次数", example = "10")
    private Integer visitCount;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
