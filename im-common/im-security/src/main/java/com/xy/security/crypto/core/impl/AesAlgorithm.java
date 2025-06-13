package com.xy.security.crypto.core.impl;


import com.xy.security.CryptoProperties;
import com.xy.security.crypto.core.CryptoAlgorithm;
import com.xy.security.crypto.domain.CryptoMode;
import com.xy.security.crypto.utils.AesUtil;
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