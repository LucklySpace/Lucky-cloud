package com.xy.lucky.platform.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "paddleocr")
@Schema(description = "PaddleOCR 服务配置")
public class PaddleOcrProperties {

    @Schema(description = "是否启用 OCR 功能")
    private boolean enabled = false;

    @Schema(description = "OCR 服务基础地址，例如 http://127.0.0.1:9000")
    private String baseUrl = "http://127.0.0.1:9000";

    @Schema(description = "OCR 识别接口路径")
    private String predictPath = "/predict/ocr_system";

    @Schema(description = "请求超时时间（毫秒）")
    private int timeoutMillis = 8000;
}
