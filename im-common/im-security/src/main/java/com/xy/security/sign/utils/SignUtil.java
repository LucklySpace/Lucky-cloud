package com.xy.security.sign.utils;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SignUtil {

    public static Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> map = new TreeMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0 && !"sign".equalsIgnoreCase(k)) {
                map.put(k, v[0]);
            }
        });
        return map;
    }

    public static String buildBaseString(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    public static String hmacSha256(String data, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(keySpec);
            byte[] macData = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(macData);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 签名异常", e);
        }
    }
}
