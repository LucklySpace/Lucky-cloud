package com.xy.lucky.core.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类
 */
@Slf4j
public class JwtUtil {

    // 建议从配置文件中注入
    private static final String KEY = "Jiawa12306";
    private static final byte[] SECRET = Md5Utils.md5(KEY).getBytes();

    // JWT 标准字段常量
    private static final String ISSUED_AT = "iat";
    private static final String EXPIRES_AT = "exp";
    private static final String NOT_BEFORE = "nbf";

    // Create HMAC signer
    private static final JWSSigner signer;

    static {
        try {
            signer = new MACSigner(SECRET);
        } catch (KeyLengthException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建 Token
     *
     * @param username   用户名
     * @param time       有效时间值（例如：30）
     * @param chronoUnit 时间单位（例如：DateField.MINUTE）
     * @return token 字符串
     */
    public static String createToken(String username, Integer time, ChronoUnit chronoUnit) {
        return createToken(username, 0L, time, chronoUnit);
    }

    /**
     * 创建带版本号的 Token
     *
     * @param username     用户名
     * @param tokenVersion 令牌版本号，用于踢人和会话失效控制
     * @param time         有效时间值（例如：30）
     * @param chronoUnit   时间单位（例如：DateField.MINUTE）
     * @return token 字符串
     */
    public static String createToken(String username, long tokenVersion, Integer time, ChronoUnit chronoUnit) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expTime = now.plus(time, chronoUnit);

            Map<String, Object> payload = new HashMap<>();
            payload.put(ISSUED_AT, Date.from(now.atZone(ZoneId.systemDefault()).toInstant()).getTime() / 1000);
            payload.put(EXPIRES_AT, Date.from(expTime.atZone(ZoneId.systemDefault()).toInstant()).getTime() / 1000);
            payload.put(NOT_BEFORE, Date.from(now.atZone(ZoneId.systemDefault()).toInstant()).getTime() / 1000);
            payload.put("username", username);
            payload.put("ver", tokenVersion);
            JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(payload));
            // Apply the HMAC
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("createToken error:", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 刷新 Token
     *
     * @param token      旧的 JWT 字符串
     * @param time       刷新后有效时间值
     * @param chronoUnit 时间单位
     * @return 新的 Token 字符串
     */
    public static String refreshToken(String token, Integer time, ChronoUnit chronoUnit) {
        try {
            // 获取旧 token 中的自定义 payload（不含时间字段）
            Map<String, Object> oldPayload = getJSONObject(token).toJSONObject();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expTime = now.plus(time, chronoUnit);

            Map<String, Object> newPayload = new HashMap<>();
            newPayload.put(ISSUED_AT, Date.from(now.atZone(ZoneId.systemDefault()).toInstant()).getTime() / 1000);
            newPayload.put(EXPIRES_AT, Date.from(expTime.atZone(ZoneId.systemDefault()).toInstant()).getTime() / 1000);
            newPayload.put(NOT_BEFORE, Date.from(now.atZone(ZoneId.systemDefault()).toInstant()).getTime() / 1000);

            // 复制旧的自定义字段
            newPayload.putAll(oldPayload);

            JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(newPayload));
            // Apply the HMAC
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (Exception e) {
            log.error("refreshToken error:", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 校验 Token 是否有效
     *
     * @param token JWT字符串
     * @return 是否有效
     */
    public static boolean validate(String token) {
        try {
            if (token == null || token.isBlank()) {
                return false;
            }
            JWSObject jwsObject = parse(token);
            JWSVerifier verifier = new MACVerifier(SECRET);
            if (!jwsObject.verify(verifier)) {
                return false;
            }

            Date now = new Date();
            Date notBefore = getNotBefore(token);
            if (notBefore != null && now.before(notBefore)) {
                return false;
            }

            Date expiresAt = getExpiresAt(token);
            if (expiresAt == null || !now.before(expiresAt)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("validate token error:", e);
            return false;
        }
    }

    /**
     * 获取用户名
     */
    public static String getUsername(String token) {
        try {
            return getPayload(token).toJSONObject().get("username").toString();
        } catch (ParseException e) {
            log.error("getUsername error:", e);
            return "";
        }
    }

    /**
     * 获取令牌版本号
     *
     * @param token JWT字符串
     * @return 版本号，如果不存在则返回0
     */
    public static long getTokenVersion(String token) {
        try {
            Object ver = getPayload(token).toJSONObject().get("ver");
            if (ver == null) {
                return 0L;
            }
            if (ver instanceof Long) {
                return (Long) ver;
            }
            if (ver instanceof Integer) {
                return ((Integer) ver).longValue();
            }
            return Long.parseLong(ver.toString());
        } catch (Exception e) {
            log.error("getTokenVersion error:", e);
            return 0L;
        }
    }

    /**
     * 获取 payload 去除时间字段
     */
    public static Payload getJSONObject(String token) throws ParseException {
        Payload payload = getPayload(token);
        // 创建新的payload map，避免修改原始payload
        Map<String, Object> payloadMap = new HashMap<>(payload.toJSONObject());
        payloadMap.remove(ISSUED_AT);
        payloadMap.remove(EXPIRES_AT);
        payloadMap.remove(NOT_BEFORE);
        return new Payload(payloadMap);
    }

    /**
     * 获取签发时间
     */
    public static Date getIssuedAt(String token) throws ParseException {
        Long timestamp = (Long) getPayload(token).toJSONObject().get(ISSUED_AT);
        return timestamp != null ? new Date(timestamp * 1000) : null;
    }

    /**
     * 获取生效时间
     */
    public static Date getNotBefore(String token) throws ParseException {
        Long timestamp = (Long) getPayload(token).toJSONObject().get(NOT_BEFORE);
        return timestamp != null ? new Date(timestamp * 1000) : null;
    }

    /**
     * 获取过期时间
     */
    public static Date getExpiresAt(String token) throws ParseException {
        Long timestamp = (Long) getPayload(token).toJSONObject().get(EXPIRES_AT);
        return timestamp != null ? new Date(timestamp * 1000) : null;
    }

    // 私有辅助方法：统一解析 token 并设置密钥
    private static JWSObject parse(String token) throws ParseException {
        return JWSObject.parse(token);
    }

    // 私有辅助方法：获取 payload
    private static Payload getPayload(String token) throws ParseException {
        return parse(token).getPayload();
    }

    /**
     * 获取剩余过期时间（毫秒）
     *
     * @param token JWT字符串
     * @return 剩余毫秒数，若已过期则返回0
     */
    public static long getRemainingMillis(String token) throws ParseException {
        Date expiresAt = getExpiresAt(token);
        if (expiresAt == null) {
            return 0;
        }
        long remaining = expiresAt.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    /**
     * 获取剩余过期时间，以指定单位返回
     *
     * @param token JWT字符串
     * @param unit  时间单位，例如 TimeUnit.SECONDS
     * @return 指定单位的剩余时间数，若已过期则返回0
     */
    public static long getRemaining(String token, TimeUnit unit) {
        try {
            long millis = getRemainingMillis(token);
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            log.error("getRemaining error:", e);
            return 0;
        }
    }
}
