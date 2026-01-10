package com.xy.lucky.crypto.config;


import com.xy.lucky.crypto.CryptoProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties({CryptoProperties.class})
public class ImCryptoAutoConfiguration {

    private final CryptoProperties properties;

    public ImCryptoAutoConfiguration(CryptoProperties properties) {
        this.properties = properties;
    }

    /**
     * 基础属性检查，提供友好提示日志
     */
    @PostConstruct
    public void validateProperties() {
        CryptoProperties.Crypto crypto = properties.getCrypto();
        if (crypto != null && crypto.getAesKey() != null) {
            int len = crypto.getAesKey().length();
            if (len != 16 && len != 24 && len != 32) {
                log.warn("AES 密钥长度为 {}，建议使用 16/24/32 位以匹配常用密钥尺寸", len);
            }
        } else {
            log.warn("未配置 AES 密钥 security.crypto.aesKey，将使用默认值，建议在生产环境显式配置");
        }
    }
}
