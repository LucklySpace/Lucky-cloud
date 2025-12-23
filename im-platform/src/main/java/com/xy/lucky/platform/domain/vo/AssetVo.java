package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 资产信息响应对象
 * 用于返回资产发布的结果信息
 */
@Data
@Schema(name = "AssetVo", description = "平台资产：包含平台、文件名、对象存储信息等")
public class AssetVo {

    @Schema(description = "发布ID")
    @NotBlank(message = "releaseId 不能为空")
    @Size(max = 64, message = "releaseId 最长 64 字符")
    private String releaseId;

    @Schema(description = "版本号", example = "1.0.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "version 不能为空")
    @Size(max = 64, message = "version 最长 64 字符")
    private String version;

    @Schema(description = "平台标识", example = "windows-x86_64", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "platform 不能为空")
    @Size(max = 64, message = "platform 最长 64 字符")
    private String platform;

    @Schema(description = "MD5")
    @NotBlank(message = "md5 不能为空")
    @Size(max = 64, message = "md5 最长 64 字符")
    private String md5;

    @Schema(description = "数字签名")
    @NotBlank(message = "signature 不能为空")
    private String signature;
}