package com.xy.lucky.crypto.core.crypto.core.impl;


import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.core.CryptoAlgorithm;
import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import com.xy.lucky.crypto.core.crypto.utils.AesUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * AES 对称加密算法实现。
 * 使用配置项中的密钥进行加解密。
 */
@Component
public class AesAlgorithm implements CryptoAlgorithm {

    @Resource
    private CryptoProperties properties;

    @Override
    public String encrypt(String plainText) throws Exception {
        return AesUtil.encrypt(plainText, properties.getCrypto().getAesKey());
    }

    @Override
    public String decrypt(String cipherText) throws Exception {
        return AesUtil.decrypt(cipherText, properties.getCrypto().getAesKey());
    }

    @Override
    public CryptoMode mode() {
        return CryptoMode.AES;
    }
}
