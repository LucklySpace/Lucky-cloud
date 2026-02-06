package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "OCR 识别请求")
public class OcrRequestVo {

    @NotEmpty
    @Schema(description = "Base64 图像数组（支持多张）")
    private List<String> images;
}
