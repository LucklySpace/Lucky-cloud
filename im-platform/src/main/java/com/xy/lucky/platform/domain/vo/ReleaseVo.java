package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布信息响应对象
 * 用于封装应用发布信息的响应数据
 */
@Data
@Schema(name = "ReleaseVo", description = "发布信息响应")
public class ReleaseVo {

    @Schema(description = "应用ID", example = "my-app")
    private String appId;

    @Schema(description = "发布ID", example = "1")
    private String releaseId;

    @Schema(description = "版本号", example = "1.0.0")
    private String version;
}