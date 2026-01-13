package com.xy.lucky.crypto.core.sign.core.impl;

import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.sign.core.SignatureAlgorithm;
import com.xy.lucky.crypto.core.sign.domain.SignatureMode;
import com.xy.lucky.crypto.core.sign.utils.SignUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HMAC-SHA512 签名算法实现
 */
@Component
public class HmacSha512Signature implements SignatureAlgorithm {

    @Resource
    private CryptoProperties properties;

    @Override
    public SignatureMode mode() {
        return SignatureMode.HMAC_SHA512;
    }

    @Override
    public String sign(Map<String, String> data) {
        String base = SignUtil.buildBaseString(data);
        return SignUtil.hmacSha512(base, properties.getSign().getSecret());
    }

    @Override
    public String sign(Map<String, String> data, String[] excludeFields) {
        Set<String> excludeSet = new HashSet<>(Arrays.asList(excludeFields));
        String base = SignUtil.buildBaseString(data, excludeSet);
        return SignUtil.hmacSha512(base, properties.getSign().getSecret());
    }

    @Override
    public boolean verify(Map<String, String> data, String signature) {
        String computed = sign(data);
        return SignUtil.safeEquals(computed, signature);
    }

    @Override
    public boolean verify(Map<String, String> data, String signature, String[] excludeFields) {
        String computed = sign(data, excludeFields);
        return SignUtil.safeEquals(computed, signature);
    }
}

