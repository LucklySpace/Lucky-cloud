package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "OCR 网络图片识别请求")
public class OcrUrlRequestVo {

    @NotEmpty
    @Schema(description = "网络图片 URL 列表（http/https）")
    private List<String> urls;
}
