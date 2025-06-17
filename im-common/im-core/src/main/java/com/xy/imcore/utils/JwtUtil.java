package com.xy.imcore.utils;


import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTException;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类
 */
public class JwtUtil {

    // 建议从配置文件中注入
    private static final String KEY = "Jiawa12306";

    /**
     * 创建 Token
     *
     * @param username 用户名
     * @param time     有效时间值（例如：30）
     * @param datePart 时间单位（例如：DateField.MINUTE）
     * @return token 字符串
     */
    public static String createToken(String username, Integer time, DateField datePart) {
        DateTime now = DateTime.now();
        DateTime expTime = now.offsetNew(datePart, time);

        Map<String, Object> payload = new HashMap<>();
        payload.put(JWTPayload.ISSUED_AT, now);
        payload.put(JWTPayload.EXPIRES_AT, expTime);
        payload.put(JWTPayload.NOT_BEFORE, now);
        payload.put("username", username);

        return JWTUtil.createToken(payload, KEY.getBytes());
    }

    /**
     * 刷新 Token
     *
     * @param token    旧的 JWT 字符串
     * @param time     刷新后有效时间值
     * @param datePart 时间单位
     * @return 新的 Token 字符串
     */
    public static String refreshToken(String token, Integer time, DateField datePart) {
        // 获取旧 token 中的自定义 payload（不含时间字段）
        JSONObject claims = getJSONObject(token);

        DateTime now = DateTime.now();
        DateTime expTime = now.offsetNew(datePart, time);

        Map<String, Object> newPayload = new HashMap<>();
        newPayload.put(JWTPayload.ISSUED_AT, now);
        newPayload.put(JWTPayload.NOT_BEFORE, now);
        newPayload.put(JWTPayload.EXPIRES_AT, expTime);
        // 复制旧的自定义字段
        for (String key : claims.keySet()) {
            newPayload.put(key, claims.get(key));
        }

        return JWTUtil.createToken(newPayload, KEY.getBytes());
    }

    /**
     * 校验 Token 是否有效
     *
     * @param token JWT字符串
     * @return 是否有效
     */
    public static boolean validate(String token) {
        try {
            JWT jwt = parse(token);
            return jwt.verify() && jwt.validate(0);
        } catch (JWTException e) {
            return false;
        }
    }

    /**
     * 获取用户名
     */
    public static String getUsername(String token) {
        return getPayload(token).getStr("username");
    }

    /**
     * 获取 payload 去除时间字段
     */
    public static JSONObject getJSONObject(String token) {
        JSONObject payload = getPayload(token);
        payload.remove(JWTPayload.ISSUED_AT);
        payload.remove(JWTPayload.EXPIRES_AT);
        payload.remove(JWTPayload.NOT_BEFORE);
        return payload;
    }

    /**
     * 获取签发时间
     */
    public static Date getIssuedAt(String token) {
        return getPayload(token).getDate(JWTPayload.ISSUED_AT);
    }

    /**
     * 获取生效时间
     */
    public static Date getNotBefore(String token) {
        return getPayload(token).getDate(JWTPayload.NOT_BEFORE);
    }

    /**
     * 获取过期时间
     */
    public static Date getExpiresAt(String token) {
        return getPayload(token).getDate(JWTPayload.EXPIRES_AT);
    }

    // 私有辅助方法：统一解析 token 并设置密钥
    private static JWT parse(String token) {
        return JWTUtil.parseToken(token).setKey(KEY.getBytes());
    }

    // 私有辅助方法：获取 payload
    private static JSONObject getPayload(String token) {
        return parse(token).getPayloads();
    }

    /**
     * 获取剩余过期时间（毫秒）
     *
     * @param token JWT字符串
     * @return 剩余毫秒数，若已过期则返回0
     */
    public static long getRemainingMillis(String token) {
        Date expiresAt = getExpiresAt(token);
        long remaining = expiresAt.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }


    /**
     * 获取剩余过期时间，以指定单位返回
     *
     * @param token JWT字符串
     * @param unit  时间单位，例如 TimeUnit.SECONDS
     * @return 指定单位的剩余时间数，若已过期则返回0
     */
    public static long getRemaining(String token, TimeUnit unit) {
        long millis = getRemainingMillis(token);
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }
}
