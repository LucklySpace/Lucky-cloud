package com.xy.lucky.platform.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "email")
@Schema(description = "邮件配置")
public class EmailProperties {

    @Schema(description = "是否启用邮件功能")
    private boolean enabled = false;

    @Schema(description = "默认供应商")
    private String defaultProvider = "smtp";

    @Schema(description = "SMTP配置")
    private Smtp smtp = new Smtp();

    @Data
    public static class Smtp {
        private String host;
        private Integer port = 587;
        private String username;
        private String password;
        private String from;
        private boolean ssl = false;
    }
}
