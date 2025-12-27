//package com.xy.lucky.crypto.core.crypto.core.impl;
//
//
//import com.xy.lucky.crypto.CryptoProperties;
//import com.xy.lucky.crypto.core.crypto.core.CryptoAlgorithm;
//import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
//import com.xy.lucky.crypto.core.crypto.utils.RsaUtil;
//import jakarta.annotation.Resource;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RsaAlgorithm implements CryptoAlgorithm {
//
//    @Resource
//    private CryptoProperties properties;
//
//    @Override
//    public String encrypt(String plainText) throws Exception {
//        return RsaUtil.encrypt(plainText, properties.getRsaPublicKey());
//    }
//
//    @Override
//    public String decrypt(String cipherText) throws Exception {
//        return RsaUtil.decrypt(cipherText, properties.getRsaPrivateKey());
//    }
//
//    @Override
//    public CryptoMode mode() {
//        return CryptoMode.RSA;
//    }
//}
