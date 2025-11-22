package com.xy.lucky.crypto.core.core.impl;


import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.core.CryptoAlgorithm;
import com.xy.lucky.crypto.core.domain.CryptoMode;
import com.xy.lucky.crypto.core.utils.KeyUtil;
import com.xy.lucky.crypto.core.utils.RsaUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RsaAlgorithm implements CryptoAlgorithm {

    @Resource
    private CryptoProperties properties;

    @Override
    public String encrypt(String plainText) throws Exception {
        return RsaUtil.encrypt(plainText, KeyUtil.getPublicKey(properties.getRsaPublicKey()));
    }

    @Override
    public String decrypt(String cipherText) throws Exception {
        return RsaUtil.decrypt(cipherText, KeyUtil.getPrivateKey(properties.getRsaPrivateKey()));
    }

    @Override
    public CryptoMode mode() {
        return CryptoMode.RSA;
    }
}