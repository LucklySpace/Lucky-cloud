package com.xy.lucky.core.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.time.Instant;
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

    // ==== 建议从配置文件读取，必须 >= 32 字节（256 bits） ====
    private static final String SECRET_KEY = "YourVerySecureSuperLongSecretKeyAtLeast32Byte!";
    private static final byte[] SECRET = SECRET_KEY.getBytes();

    // 标准字段
    private static final String ISSUED_AT = "iat";
    private static final String EXPIRES_AT = "exp";
    private static final String NOT_BEFORE = "nbf";

    // 签名器
    private static final JWSSigner SIGNER;
    static {
        try {
            SIGNER = new MACSigner(SECRET);
        } catch (KeyLengthException e) {
            throw new IllegalStateException("JWT 签名器初始化失败: 密钥长度不足", e);
        }
    }

    /**
     * 创建 Token
     */
    public static String createToken(String username, long version,
                                     long amount, ChronoUnit unit) {
        try {
            Instant now = Instant.now(); // UTC
            Instant exp = now.plus(amount, unit);

            Map<String, Object> payload = new HashMap<>();
            payload.put(ISSUED_AT, now.getEpochSecond());
            payload.put(NOT_BEFORE, now.getEpochSecond());
            payload.put(EXPIRES_AT, exp.getEpochSecond());
            payload.put("username", username);
            payload.put("ver", version);

            JWSObject jws = new JWSObject(
                    new JWSHeader(JWSAlgorithm.HS256),
                    new Payload(payload)
            );
            jws.sign(SIGNER);
            return jws.serialize();
        } catch (Exception e) {
            log.error("createToken error:", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 刷新 Token
     */
    public static String refreshToken(String token, long amount, ChronoUnit unit) {
        try {
            Map<String, Object> old = getPayload(token).toJSONObject();
            old.remove(ISSUED_AT);
            old.remove(NOT_BEFORE);
            old.remove(EXPIRES_AT);

            Instant now = Instant.now();
            Instant exp = now.plus(amount, unit);

            old.put(ISSUED_AT, now.getEpochSecond());
            old.put(NOT_BEFORE, now.getEpochSecond());
            old.put(EXPIRES_AT, exp.getEpochSecond());

            JWSObject jws = new JWSObject(
                    new JWSHeader(JWSAlgorithm.HS256),
                    new Payload(old)
            );
            jws.sign(SIGNER);
            return jws.serialize();
        } catch (Exception e) {
            log.error("refreshToken error:", e);
            throw new RuntimeException(e);
        }
    }

    /** 验证是否有效 */
    public static boolean validate(String token) {
        try {
            if (token == null || token.isBlank()) return false;

            JWSObject jwsObject = JWSObject.parse(token);
            if (!jwsObject.verify(new MACVerifier(SECRET))) {
                return false;
            }

            Instant now = Instant.now();
            Instant nbf = Instant.ofEpochSecond(getNotBefore(token).toInstant().getEpochSecond());
            Instant exp = Instant.ofEpochSecond(getExpiresAt(token).toInstant().getEpochSecond());

            return !now.isBefore(nbf) && now.isBefore(exp);
        } catch (Exception e) {
            log.error("validate token error:", e);
            return false;
        }
    }

    public static String getUsername(String token) {
        try {
            return getPayload(token).toJSONObject().get("username").toString();
        } catch (ParseException e) {
            log.error("getUsername error:", e);
            return null;
        }
    }

    public static long getTokenVersion(String token) {
        try {
            Object ver = getPayload(token).toJSONObject().get("ver");
            return ver == null ? 0L : Long.parseLong(ver.toString());
        } catch (Exception e) {
            log.error("getTokenVersion error:", e);
            return 0L;
        }
    }

    public static Date getIssuedAt(String token) throws ParseException {
        Long ts = (Long) getPayload(token).toJSONObject().get(ISSUED_AT);
        return ts == null ? null : Date.from(Instant.ofEpochSecond(ts));
    }

    public static Date getNotBefore(String token) throws ParseException {
        Long ts = (Long) getPayload(token).toJSONObject().get(NOT_BEFORE);
        return ts == null ? null : Date.from(Instant.ofEpochSecond(ts));
    }

    public static Date getExpiresAt(String token) throws ParseException {
        Long ts = (Long) getPayload(token).toJSONObject().get(EXPIRES_AT);
        return ts == null ? null : Date.from(Instant.ofEpochSecond(ts));
    }

    private static Payload getPayload(String token) throws ParseException {
        return JWSObject.parse(token).getPayload();
    }

    /**
     * 获取剩余过期时间（毫秒）
     */
    public static long getRemainingMillis(String token) {
        try {
            long exp = getExpiresAt(token).getTime();
            long now = Instant.now().toEpochMilli();
            return Math.max(exp - now, 0);
        } catch (Exception e) {
            log.error("getRemainingMillis error:", e);
            return 0;
        }
    }

    public static long getRemaining(String token, TimeUnit unit) {
        long ms = getRemainingMillis(token);
        return unit.convert(ms, TimeUnit.MILLISECONDS);
    }
}