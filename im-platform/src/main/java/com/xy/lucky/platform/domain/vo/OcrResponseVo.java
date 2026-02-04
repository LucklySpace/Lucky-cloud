package com.xy.lucky.platform.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "OCR 识别响应")
public class OcrResponseVo {

    @Schema(description = "状态码，例如 000 表示成功")
    private String status;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "识别结果，按图像顺序嵌套")
    private List<List<OcrItemVo>> results;
}
