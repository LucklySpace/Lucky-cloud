package com.xy.lucky.crypto.core.sign.core.impl;

import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.sign.core.SignatureAlgorithm;
import com.xy.lucky.crypto.core.sign.domain.SignatureMode;
import com.xy.lucky.crypto.core.sign.utils.SignUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HmacSha256Signature implements SignatureAlgorithm {

    @Resource
    private CryptoProperties properties;

    @Override
    public SignatureMode mode() {
        return SignatureMode.HMAC_SHA256;
    }

    @Override
    public String sign(Map<String, String> data) {
        String base = SignUtil.buildBaseString(data);
        return SignUtil.hmacSha256(base, properties.getSign().getSecret());
    }

    @Override
    public boolean verify(Map<String, String> data, String signature) {
        return sign(data).equalsIgnoreCase(signature);
    }
}
