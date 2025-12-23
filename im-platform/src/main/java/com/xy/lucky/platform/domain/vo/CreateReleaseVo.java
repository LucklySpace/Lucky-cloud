package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建发布请求对象
 * 用于创建新的应用版本发布
 */
@Data
@Schema(name = "CreateReleaseVo", description = "创建发布请求")
public class CreateReleaseVo {

    @Schema(description = "应用ID", example = "my-app")
    private String appId;

    @Schema(description = "发布版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.0.0")
    @NotBlank(message = "版本号不能为空")
    @Size(max = 64, message = "版本号最长64字符")
    private String version;

    @Schema(description = "更新说明", example = "修复问题并优化体验")
    @Size(max = 5000, message = "更新说明最长5000字符")
    private String notes;

    @Schema(description = "发布时间（ISO 8601）", example = "2025-12-22T10:00:00Z")
    @Size(max = 64, message = "发布时间最长64字符")
    private String pubDate;
}