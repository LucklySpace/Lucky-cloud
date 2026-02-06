package com.xy.lucky.rpc.api.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短链信息 VO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ShortLinkVo", description = "短链信息响应")
public class ShortLinkVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原始URL", example = "https://example.com/docs/123")
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
