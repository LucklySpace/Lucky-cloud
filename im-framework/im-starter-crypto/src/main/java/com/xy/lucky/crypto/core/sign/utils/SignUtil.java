package com.xy.lucky.crypto.core.sign.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 签名工具类
 * <p>
 * 特性：
 * - 支持 URL 参数签名
 * - 支持 JSON Body 签名
 * - 支持多种 HMAC 算法
 * - 安全的时间比较
 */
public final class SignUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SignUtil() {
    }

    // ==================== 参数提取 ====================

    /**
     * 从 HTTP 请求中提取 URL 参数
     */
    public static Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> map = new TreeMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0 && !"sign".equalsIgnoreCase(k)) {
                map.put(k, v[0]);
            }
        });
        return map;
    }

    /**
     * 从请求体中提取 JSON 参数
     */
    public static Map<String, String> getJsonParams(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            if (body.isEmpty()) {
                return new TreeMap<>();
            }
            return parseJson(body);
        } catch (Exception e) {
            return new TreeMap<>();
        }
    }

    /**
     * 解析 JSON 字符串为 Map
     */
    public static Map<String, String> parseJson(String json) {
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
            return flattenMap(map, "");
        } catch (Exception e) {
            return new TreeMap<>();
        }
    }

    /**
     * 将嵌套 Map 展平为单层 Map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> flattenMap(Map<String, Object> map, String prefix) {
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            } else if (value instanceof Map) {
                result.putAll(flattenMap((Map<String, Object>) value, key));
            } else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        result.putAll(flattenMap((Map<String, Object>) item, key + "[" + i + "]"));
                    } else if (item != null) {
                        result.put(key + "[" + i + "]", String.valueOf(item));
                    }
                }
            } else {
                result.put(key, String.valueOf(value));
            }
        }
        return result;
    }

    // ==================== 签名构建 ====================

    /**
     * 构建待签名字符串
     * 格式: key1=value1&key2=value2...（按 key 字典序排列）
     */
    public static String buildBaseString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    /**
     * 构建待签名字符串（带排除字段）
     */
    public static String buildBaseString(Map<String, String> params, Set<String> excludeFields) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !excludeFields.contains(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    /**
     * 构建待签名字符串（只包含指定字段）
     */
    public static String buildBaseStringInclude(Map<String, String> params, Set<String> includeFields) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> includeFields.isEmpty() || includeFields.contains(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    // ==================== HMAC 签名 ====================

    /**
     * HMAC-SHA256 签名
     */
    public static String hmacSha256(String data, String secret) {
        return hmac(data, secret, "HmacSHA256");
    }

    /**
     * HMAC-SHA512 签名
     */
    public static String hmacSha512(String data, String secret) {
        return hmac(data, secret, "HmacSHA512");
    }

    /**
     * 通用 HMAC 签名
     */
    public static String hmac(String data, String secret, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(keySpec);
            byte[] macData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(macData);
        } catch (Exception e) {
            throw new RuntimeException(algorithm + " 签名异常", e);
        }
    }

    /**
     * MD5 签名（不推荐，仅用于兼容旧系统）
     */
    public static String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("MD5 签名异常", e);
        }
    }

    /**
     * SHA256 签名
     */
    public static String sha256(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA256 签名异常", e);
        }
    }

    // ==================== 验签工具 ====================

    /**
     * 安全的签名比较（防止时序攻击）
     */
    public static boolean safeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * 验证时间戳是否在容差范围内
     *
     * @param timestamp       请求时间戳（毫秒）
     * @param toleranceMillis 容差毫秒数
     * @return 是否有效
     */
    public static boolean isTimestampValid(long timestamp, long toleranceMillis) {
        long now = System.currentTimeMillis();
        return Math.abs(now - timestamp) <= toleranceMillis;
    }

    /**
     * 生成随机 nonce
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成当前时间戳（毫秒）
     */
    public static long generateTimestamp() {
        return System.currentTimeMillis();
    }
}
