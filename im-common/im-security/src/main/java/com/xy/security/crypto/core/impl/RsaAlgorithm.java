package com.xy.security.crypto.core.impl;


import com.xy.security.CryptoProperties;
import com.xy.security.crypto.core.CryptoAlgorithm;
import com.xy.security.crypto.domain.CryptoMode;
import com.xy.security.crypto.utils.KeyUtil;
import com.xy.security.crypto.utils.RsaUtil;
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