package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语言包视图对象
 * - 对齐前端 LanguagePackMeta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "语言包元信息")
public class LanguagePackVo {

    @Schema(description = "地区/语言标识", example = "zh-CN")
    private String locale;

    @Schema(description = "名称", example = "简体中文")
    private String name;

    @Schema(description = "版本号", example = "1.0.0")
    private String version;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "下载地址")
    private String downloadUrl;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "创建时间")
    private String createTime;
}

