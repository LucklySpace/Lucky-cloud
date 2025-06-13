package com.xy.security.sign.core.impl;

import com.xy.security.CryptoProperties;
import com.xy.security.sign.core.SignatureAlgorithm;
import com.xy.security.sign.domain.SignatureMode;
import com.xy.security.sign.utils.SignUtil;
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
