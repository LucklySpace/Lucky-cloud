package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资产信息响应对象
 * 用于返回资产发布的结果信息
 */
@Data
@Schema(name = "AssetVo", description = "资产信息响应")
public class AssetVo {

    @Schema(description = "应用ID", example = "my-app")
    private String appId;

    @Schema(description = "发布ID", example = "1")
    private String releaseId;

    @Schema(description = "平台标识", example = "windows-x86_64")
    private String platform;

    @Schema(description = "下载链接", example = "https://example.com/my-app-1.0.0-windows-x86_64.zip")
    private String url;
}