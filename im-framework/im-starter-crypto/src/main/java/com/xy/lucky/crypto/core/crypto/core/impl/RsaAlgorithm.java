package com.xy.lucky.crypto.core.crypto.core.impl;

import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.core.CryptoAlgorithm;
import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import com.xy.lucky.crypto.core.crypto.utils.RsaUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RSA 非对称加密算法实现
 * <p>
 * 使用公钥加密，私钥解密
 * 适用于密钥交换、短数据加密场景
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "security.crypto", name = "rsa-public-key")
public class RsaAlgorithm implements CryptoAlgorithm {

    @Resource
    private CryptoProperties properties;

    @PostConstruct
    public void init() {
        CryptoProperties.Crypto crypto = properties.getCrypto();
        if (!StringUtils.hasText(crypto.getRsaPublicKey())) {
            log.warn("RSA 公钥未配置，RSA 加密功能不可用");
        }
        if (!StringUtils.hasText(crypto.getRsaPrivateKey())) {
            log.warn("RSA 私钥未配置，RSA 解密功能不可用");
        }
    }

    @Override
    public String encrypt(String plainText) throws Exception {
        String publicKey = properties.getCrypto().getRsaPublicKey();
        if (!StringUtils.hasText(publicKey)) {
            throw new IllegalStateException("RSA 公钥未配置，无法执行加密");
        }

        if (properties.getCrypto().isDebug()) {
            log.debug("RSA 公钥加密");
        }

        return RsaUtil.encrypt(plainText, publicKey);
    }

    @Override
    public String decrypt(String cipherText) throws Exception {
        String privateKey = properties.getCrypto().getRsaPrivateKey();
        if (!StringUtils.hasText(privateKey)) {
            throw new IllegalStateException("RSA 私钥未配置，无法执行解密");
        }

        if (properties.getCrypto().isDebug()) {
            log.debug("RSA 私钥解密");
        }

        return RsaUtil.decrypt(cipherText, privateKey);
    }

    @Override
    public CryptoMode mode() {
        return CryptoMode.RSA;
    }
}
