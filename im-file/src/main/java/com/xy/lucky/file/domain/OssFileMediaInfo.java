package com.xy.lucky.file.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.geometry.Positions;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "媒体文件信息")
public class OssFileMediaInfo {

    @Schema(description = "宽 高")
    private Integer width, height;

    @Schema(description = "水印地址")
    private String watermarkPath;

    @Schema(description = "水印位置")
    private Positions watermarkPosition = Positions.BOTTOM_RIGHT;

    @Schema(description = "透明度")
    private Float opacity = 0.5f;

    @Schema(description = "放大倍数")
    private Double scale = 0.5;

    @Schema(description = "比例")
    private Double ratio = 0.3;

    @Schema(description = "格式")
    private String format = "png";

}
