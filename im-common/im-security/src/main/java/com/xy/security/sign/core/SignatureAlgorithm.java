package com.xy.security.sign.core;

import com.xy.security.sign.domain.SignatureMode;

import java.util.Map;

public interface SignatureAlgorithm {

    SignatureMode mode();

    String sign(Map<String, String> data) throws Exception;

    boolean verify(Map<String, String> data, String signature) throws Exception;
}