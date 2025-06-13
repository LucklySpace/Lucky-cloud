package com.xy.security;

import com.xy.security.crypto.domain.CryptoMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * 加解密配置属性类，支持从 application.yml 中读取配置
 */
@Data
@ConfigurationProperties(prefix = "security")
public class CryptoProperties {

    private Crypto crypto;

    private Sign sign;

    /**
     * RSA 公钥（Base64 或 PEM 格式）
     */
    private String rsaPublicKey;

    /**
     * RSA 私钥（Base64 或 PEM 格式）
     */
    private String rsaPrivateKey;

    @Data
    public static class Crypto {
        /**
         * 默认加密模式（用于响应）
         */
        private CryptoMode defaultEncrypt = CryptoMode.AES;

        /**
         * 默认解密模式（用于请求）
         */
        private CryptoMode defaultDecrypt = CryptoMode.AES;

        /**
         * AES 密钥（16位）
         */
        private String aesKey = "defaultaeskey123";
    }


    @Data
    public static class Sign {

        /**
         * 密钥
         */
        private String secret;

    }

}