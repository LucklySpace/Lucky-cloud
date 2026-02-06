package com.xy.lucky.platform.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sms")
@Schema(description = "短信配置")
public class SmsProperties {

    @Schema(description = "是否启用短信功能")
    private boolean enabled = false;

    @Schema(description = "默认供应商")
    private String defaultProvider = "zhenzi";

    @Schema(description = "榛子短信配置")
    private Zhenzi zhenzi = new Zhenzi();

    @Data
    public static class Zhenzi {
        private String apiUrl;
        private String appId;
        private String appSecret;
        private String defaultTemplateId = "10120";
    }
}
