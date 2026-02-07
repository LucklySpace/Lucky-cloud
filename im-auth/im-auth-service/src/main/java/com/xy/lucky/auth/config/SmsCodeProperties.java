package com.xy.lucky.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sms.code")
public class SmsCodeProperties {

    private Duration ttl = Duration.ofMinutes(5);

    private int maxRetry = 3;

    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private Duration window = Duration.ofMinutes(1);
        private int phoneLimit = 3;
        private int ipLimit = 20;
        private int deviceLimit = 10;
        private Duration blockDuration = Duration.ofMinutes(10);
    }
}

