package com.xy.lucky.crypto.core.crypto.core.impl;

import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.core.CryptoAlgorithm;
import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import com.xy.lucky.crypto.core.crypto.utils.AesUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AES 对称加密算法实现
 * <p>
 * 根据配置支持 GCM/CBC/ECB 三种模式
 * 推荐使用 GCM 模式，提供认证加密功能
 */
@Slf4j
@Component
public class AesAlgorithm implements CryptoAlgorithm {

    @Resource
    private CryptoProperties properties;

    @Override
    public String encrypt(String plainText) throws Exception {
        String key = properties.getCrypto().getAesKey();
        CryptoProperties.AesMode mode = properties.getCrypto().getAesMode();

        if (properties.getCrypto().isDebug()) {
            log.debug("AES 加密, 模式: {}", mode);
        }

        return switch (mode) {
            case GCM -> AesUtil.encryptGcm(plainText, key);
            case CBC -> AesUtil.encryptCbc(plainText, key);
            case ECB -> AesUtil.encryptEcb(plainText, key);
        };
    }

    @Override
    public String decrypt(String cipherText) throws Exception {
        String key = properties.getCrypto().getAesKey();
        CryptoProperties.AesMode mode = properties.getCrypto().getAesMode();

        if (properties.getCrypto().isDebug()) {
            log.debug("AES 解密, 模式: {}", mode);
        }

        return switch (mode) {
            case GCM -> AesUtil.decryptGcm(cipherText, key);
            case CBC -> AesUtil.decryptCbc(cipherText, key);
            case ECB -> AesUtil.decryptEcb(cipherText, key);
        };
    }

    @Override
    public CryptoMode mode() {
        return CryptoMode.AES;
    }
}
