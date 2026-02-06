package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布信息响应对象
 * 用于封装应用发布信息的响应数据
 */
@Data
@Schema(name = "ReleaseVo", description = "创建发布请求")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseVo {

    @Schema(description = "应用ID", example = "my-app")
    @NotBlank(message = "应用ID不能为空")
    @Size(max = 64, message = "应用ID最长64字符")
    private String appId;

    @Schema(description = "发布版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.0.0")
    @NotBlank(message = "版本号不能为空")
    @Size(max = 64, message = "版本号最长64字符")
    private String version;

    @Schema(description = "发布ID", example = "a93e7d99-76aa-4515-b43f-7614c8d410ad")
    private String releaseId;

    @Schema(description = "版本更新说明", example = "修复若干问题并优化体验")
    @Size(max = 5000, message = "更新说明最长5000字符")
    private String notes;

    @Schema(description = "发布时间（ISO 8601）", example = "2025-12-22T10:00:00Z")
    @Size(max = 64, message = "发布时间最长64字符")
    private String pubDate;
}