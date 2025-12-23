package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建短链请求
 */
@Data
@Schema(name = "CreateShortLinkVo", description = "创建短链请求参数")
public class CreateShortLinkVo {

    /**
     * 原始URL
     */
    @Schema(description = "原始URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/docs/123")
    @NotBlank(message = "originalUrl 不能为空")
    @Size(max = 2048, message = "originalUrl 最长 2048 字符")
    private String originalUrl;

    /**
     * 过期时间（秒），默认不设置表示永久有效
     */
    @Schema(description = "可选：过期时间（秒），默认永久有效", example = "86400")
    private Long expireSeconds;

    /**
     * 可选：自定义短码（只能包含字母数字，长度 4-32）
     */
    @Schema(description = "可选：自定义短码（字母数字，长度 4-32）", example = "myCode123")
    @Size(min = 4, max = 32, message = "customCode 长度需在 4-32 之间")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "customCode 只能包含字母与数字")
    private String customCode;

    /**
     * 可选：用户ID，用于生成与用户隔离的确定性短码
     */
    @Schema(description = "可选：用户ID，用于生成与用户隔离的确定性短码", example = "user-001")
    @Size(max = 128, message = "userId 最长 128 字符")
    private String userId;
}
