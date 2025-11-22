package com.xy.lucky.crypto.core.core.impl;


import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.core.CryptoAlgorithm;
import com.xy.lucky.crypto.core.domain.CryptoMode;
import com.xy.lucky.crypto.core.utils.AesUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * AES 算法
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