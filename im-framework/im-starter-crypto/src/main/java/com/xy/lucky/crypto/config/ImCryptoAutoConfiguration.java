package com.xy.lucky.crypto.config;

import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.annotation.advice.DecryptRequestBodyAdvice;
import com.xy.lucky.crypto.core.crypto.annotation.advice.EncryptResponseBodyAdvice;
import com.xy.lucky.crypto.core.crypto.core.CryptoExecutor;
import com.xy.lucky.crypto.core.crypto.core.impl.AesAlgorithm;
import com.xy.lucky.crypto.core.sign.annotation.aspect.SignatureAspect;
import com.xy.lucky.crypto.core.sign.core.impl.HmacSha256Signature;
import com.xy.lucky.crypto.core.sign.core.impl.HmacSha512Signature;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 加解密模块自动配置类
 * <p>
 * 功能特性：
 * <ul>
 *   <li>支持 AES/RSA/SM4 多种加密算法</li>
 *   <li>支持接口级别的请求解密和响应加密</li>
 *   <li>支持 HMAC-SHA256/SHA512 签名验签</li>
 *   <li>支持类级别和方法级别注解</li>
 *   <li>支持时间戳和 nonce 防重放</li>
 *   <li>高性能：密钥缓存、Cipher 复用</li>
 * </ul>
 * <p>
 * 配置示例：
 * <pre>
 * security:
 *   enabled: true
 *   crypto:
 *     enabled: true
 *     aes-key: your-16-24-32-byte-key
 *     aes-mode: GCM
 *   sign:
 *     enabled: true
 *     secret: your-secret-key
 * </pre>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CryptoProperties.class)
@ComponentScan(basePackages = {
        "com.xy.lucky.crypto.core.crypto",
        "com.xy.lucky.crypto.core.sign"
})
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
        log.info("========== 加解密模块初始化 ==========");

        // 加密配置检查
        CryptoProperties.Crypto crypto = properties.getCrypto();
        if (crypto != null && crypto.isEnabled()) {
            // AES 密钥检查
            if (crypto.getAesKey() != null) {
                int len = crypto.getAesKey().length();
                if (len != 16 && len != 24 && len != 32) {
                    log.warn("⚠ AES 密钥长度为 {}，建议使用 16/24/32 位", len);
                } else {
                    log.info("✓ AES 加密已启用，模式: {}", crypto.getAesMode());
                }
            } else {
                log.warn("⚠ 未配置 AES 密钥，将使用默认值，建议在生产环境显式配置");
            }

            // RSA 检查
            if (crypto.getRsaPublicKey() != null) {
                log.info("✓ RSA 非对称加密已启用");
            }

            // 排除路径
            if (!crypto.getExcludePaths().isEmpty()) {
                log.info("✓ 加解密排除路径: {}", crypto.getExcludePaths());
            }
        } else {
            log.info("○ 加解密功能未启用");
        }

        // 签名配置检查
        CryptoProperties.Sign sign = properties.getSign();
        if (sign != null && sign.isEnabled()) {
            if (sign.getSecret() != null) {
                log.info("✓ 签名验签已启用，算法: {}", sign.getMode());
                if (sign.isNonceEnabled()) {
                    log.info("✓ 防重放 nonce 已启用，过期时间: {}秒", sign.getNonceExpireSeconds());
                }
            } else {
                log.warn("⚠ 签名密钥未配置，签名功能不可用");
            }
        } else {
            log.info("○ 签名验签功能未启用");
        }

        log.info("======================================");
    }

    /**
     * 加解密执行器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CryptoExecutor cryptoExecutor() {
        return new CryptoExecutor(java.util.Collections.emptyList());
    }

    /**
     * AES 算法实现
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AesAlgorithm aesAlgorithm() {
        return new AesAlgorithm();
    }

    /**
     * 请求解密 Advice
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DecryptRequestBodyAdvice decryptRequestBodyAdvice() {
        return new DecryptRequestBodyAdvice();
    }

    /**
     * 响应加密 Advice
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EncryptResponseBodyAdvice encryptResponseBodyAdvice() {
        return new EncryptResponseBodyAdvice();
    }

    /**
     * HMAC-SHA256 签名实现
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.sign", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HmacSha256Signature hmacSha256Signature() {
        return new HmacSha256Signature();
    }

    /**
     * HMAC-SHA512 签名实现
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.sign", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HmacSha512Signature hmacSha512Signature() {
        return new HmacSha512Signature();
    }

    /**
     * 签名切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.sign", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SignatureAspect signatureAspect() {
        return new SignatureAspect(java.util.Collections.emptyList());
    }
}
