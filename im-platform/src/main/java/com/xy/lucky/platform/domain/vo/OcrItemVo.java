package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "OCR 识别项")
public class OcrItemVo {

    @Schema(description = "识别置信度")
    private Double confidence;

    @Schema(description = "识别文本")
    private String text;

    @JsonProperty("text_region")
    @Schema(description = "文本区域坐标列表")
    private List<List<Integer>> textRegion;
}
