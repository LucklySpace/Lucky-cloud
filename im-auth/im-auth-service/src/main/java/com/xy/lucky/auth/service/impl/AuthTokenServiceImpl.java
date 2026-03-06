package com.xy.lucky.auth.service.impl;

import com.xy.lucky.auth.domain.AuthTokenPair;
import com.xy.lucky.auth.service.AuthTokenService;
import com.xy.lucky.auth.service.TokenVersionService;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.domain.po.ImAuthTokenPo;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.rpc.api.database.auth.ImAuthTokenDubboService;
import com.xy.lucky.security.SecurityAuthProperties;
import com.xy.lucky.security.exception.AuthenticationFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    // ==================== Redis Key 前缀定义 ====================

    private static final String USER_TOKEN_META_KEY = "auth:token:meta:";

    /**
     * 令牌黑名单
     */
    private static final String BLACKLIST_KEY = "auth:token:blacklist:";

    // ==================== 依赖注入 ====================

    private final RedisCache redisCache;
    private final SecurityAuthProperties authProperties;
    private final TokenVersionService tokenVersionService;

    @DubboReference
    private ImAuthTokenDubboService authTokenDubboService;

    // ==================== 令牌签发 ====================

    @Override
    public AuthTokenPair issueTokens(String userId, String deviceId, String clientIp) {
        Objects.requireNonNull(userId, "userId 不能为空");
        return createTokenPair(userId, deviceId, clientIp);
    }

    private AuthTokenPair createTokenPair(String userId, String deviceId, String clientIp) {
        int accessTtlMinutes = Optional.ofNullable(authProperties.getExpiration()).orElse(30);
        int refreshTtlHours = Optional.ofNullable(authProperties.getRefreshExpiration()).orElse(720);
        long accessExpiresIn = TimeUnit.MINUTES.toSeconds(accessTtlMinutes);
        long refreshExpiresIn = TimeUnit.HOURS.toSeconds(refreshTtlHours);
        long tokenVersion = tokenVersionService.getCurrentVersion(userId);
        long now = System.currentTimeMillis();

        String accessToken = JwtUtil.createToken(userId, tokenVersion, accessTtlMinutes, ChronoUnit.MINUTES);
        String refreshToken = JwtUtil.createToken(userId, tokenVersion, refreshTtlHours, ChronoUnit.HOURS);

        cacheUserTokenMeta(userId, deviceId, tokenVersion, now, accessExpiresIn, refreshExpiresIn, accessToken, refreshToken);
        persistTokenRecord(userId, deviceId, clientIp, tokenVersion, now, accessExpiresIn, accessToken, refreshToken);

        log.info("令牌签发成功：userId={}, deviceId={}, version={}, ip={}",
                userId, deviceId, tokenVersion, clientIp);

        return new AuthTokenPair()
                .setUserId(userId)
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setAccessExpiresIn(accessExpiresIn)
                .setRefreshExpiresIn(refreshExpiresIn);
    }

    // ==================== 令牌刷新 ====================

    @Override
    public AuthTokenPair refreshTokens(String refreshToken, String clientIp, String deviceId) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("刷新令牌为空");
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        if (isTokenBlacklisted(refreshToken)) {
            log.warn("刷新令牌已被撤销：{}", maskToken(refreshToken));
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
        }

        if (!JwtUtil.validate(refreshToken)) {
            log.warn("刷新令牌不存在或已过期：{}", maskToken(refreshToken));
            throw new AuthenticationFailException(ResultCode.TOKEN_EXPIRED);
        }

        String userId = JwtUtil.getUsername(refreshToken);
        long tokenVersion = JwtUtil.getTokenVersion(refreshToken);
        if (!StringUtils.hasText(userId)) {
            log.warn("刷新令牌无法解析用户信息：{}", maskToken(refreshToken));
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
        }

        if (!tokenVersionService.isVersionValid(userId, tokenVersion)) {
            log.warn("令牌版本已失效：userId={}, tokenVersion={}", userId, tokenVersion);
            throw new AuthenticationFailException(ResultCode.TOKEN_VERSION_INVALID);
        }

        Map<String, Object> tokenMeta = redisCache.get(USER_TOKEN_META_KEY + userId);
        if (tokenMeta == null) {
            log.warn("刷新令牌不存在或已过期：{}", maskToken(refreshToken));
            throw new AuthenticationFailException(ResultCode.TOKEN_EXPIRED);
        }

        Object refreshTokenValue = tokenMeta.get("refreshToken");
        String storedRefreshToken = refreshTokenValue == null ? null : refreshTokenValue.toString();
        if (!refreshToken.equals(storedRefreshToken)) {
            log.warn("刷新令牌不匹配：{}", maskToken(refreshToken));
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
        }

        Object deviceIdValue = tokenMeta.get("deviceId");
        String storedDeviceId = deviceIdValue == null ? null : deviceIdValue.toString();
        String boundDeviceId = StringUtils.hasText(deviceId) ? deviceId : storedDeviceId;
        if (StringUtils.hasText(storedDeviceId)
                && StringUtils.hasText(deviceId)
                && !storedDeviceId.equals(deviceId)) {
            log.warn("设备绑定校验失败：storedDevice={}, requestDevice={}",
                    storedDeviceId, deviceId);
            throw new AuthenticationFailException(ResultCode.DEVICE_MISMATCH);
        }

        boolean reuseRefreshToken = Boolean.TRUE.equals(authProperties.getReuseRefreshTokens());
        if (reuseRefreshToken) {
            int accessTtlMinutes = Optional.ofNullable(authProperties.getExpiration()).orElse(30);
            long accessExpiresIn = TimeUnit.MINUTES.toSeconds(accessTtlMinutes);
            long refreshExpiresIn = JwtUtil.getRemaining(refreshToken, TimeUnit.SECONDS);
            if (refreshExpiresIn <= 0) {
                log.warn("刷新令牌不存在或已过期：{}", maskToken(refreshToken));
                throw new AuthenticationFailException(ResultCode.TOKEN_EXPIRED);
            }

            long now = System.currentTimeMillis();
            String accessToken = JwtUtil.createToken(userId, tokenVersion, accessTtlMinutes, ChronoUnit.MINUTES);
            cacheUserTokenMeta(userId, boundDeviceId, tokenVersion, now, accessExpiresIn, refreshExpiresIn, accessToken, refreshToken);
            persistTokenRecord(userId, boundDeviceId, clientIp, tokenVersion, now, accessExpiresIn, accessToken, refreshToken);

            log.info("令牌刷新成功：userId={}", userId);

            return new AuthTokenPair()
                    .setUserId(userId)
                    .setAccessToken(accessToken)
                    .setRefreshToken(refreshToken)
                    .setAccessExpiresIn(accessExpiresIn)
                    .setRefreshExpiresIn(refreshExpiresIn);
        } else {
            AuthTokenPair newPair = createTokenPair(userId, boundDeviceId, clientIp);
            blacklistToken(refreshToken, true);
            redisCache.del(USER_TOKEN_META_KEY + userId);
            log.info("令牌刷新成功：userId={}", userId);
            return newPair;
        }
    }

    // ==================== 令牌撤销 ====================

    @Override
    public void revokeTokens(String accessToken, String refreshToken) {
        if (StringUtils.hasText(accessToken)) {
            revokeAccessToken(accessToken);
        }

        if (StringUtils.hasText(refreshToken)) {
            revokeRefreshToken(refreshToken);
        }
    }

    /**
     * 撤销访问令牌
     */
    private void revokeAccessToken(String accessToken) {
        long ttlSeconds = JwtUtil.getRemaining(accessToken, TimeUnit.SECONDS);
        if (ttlSeconds <= 0) {
            ttlSeconds = TimeUnit.MINUTES.toSeconds(
                    Optional.ofNullable(authProperties.getExpiration()).orElse(30));
        }

        redisCache.set(BLACKLIST_KEY + accessToken, System.currentTimeMillis(), ttlSeconds, TimeUnit.SECONDS);
        String userId = JwtUtil.getUsername(accessToken);
        if (StringUtils.hasText(userId)) {
            redisCache.del(USER_TOKEN_META_KEY + userId);
        }
        log.debug("访问令牌已撤销：{}", maskToken(accessToken));
    }

    /**
     * 撤销刷新令牌
     */
    private void revokeRefreshToken(String refreshToken) {
        long ttlSeconds = JwtUtil.getRemaining(refreshToken, TimeUnit.SECONDS);
        if (ttlSeconds <= 0) {
            ttlSeconds = TimeUnit.HOURS.toSeconds(
                    Optional.ofNullable(authProperties.getRefreshExpiration()).orElse(720));
        }

        redisCache.set(BLACKLIST_KEY + refreshToken, System.currentTimeMillis(), ttlSeconds, TimeUnit.SECONDS);
        String userId = JwtUtil.getUsername(refreshToken);
        if (StringUtils.hasText(userId)) {
            redisCache.del(USER_TOKEN_META_KEY + userId);
        }
        log.debug("刷新令牌已撤销：{}", maskToken(refreshToken));
    }

    /**
     * 将令牌加入黑名单
     */
    private void blacklistToken(String token, boolean isRefreshToken) {
        long ttlSeconds;
        if (isRefreshToken) {
            ttlSeconds = TimeUnit.HOURS.toSeconds(
                    Optional.ofNullable(authProperties.getRefreshExpiration()).orElse(720));
        } else {
            ttlSeconds = TimeUnit.MINUTES.toSeconds(
                    Optional.ofNullable(authProperties.getExpiration()).orElse(30));
        }

        redisCache.set(BLACKLIST_KEY + token, System.currentTimeMillis(), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        return redisCache.hasKey(BLACKLIST_KEY + token);
    }

    // ==================== 令牌解析 ====================

    @Override
    public Optional<String> resolveAccessToken(String headerValue, String paramValue) {
        return resolveToken(headerValue, paramValue);
    }

    @Override
    public Optional<String> resolveRefreshToken(String headerValue, String paramValue) {
        return resolveToken(headerValue, paramValue);
    }

    @Override
    public boolean isAccessTokenValid(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }
        if (isTokenBlacklisted(accessToken)) {
            return false;
        }
        if (!JwtUtil.validate(accessToken)) {
            return false;
        }
        String userId = JwtUtil.getUsername(accessToken);
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        long tokenVersion = JwtUtil.getTokenVersion(accessToken);
        return tokenVersionService.isVersionValid(userId, tokenVersion);
    }

    private Optional<String> resolveToken(String headerValue, String paramValue) {
        if (StringUtils.hasText(headerValue)) {
            return Optional.of(stripBearer(headerValue));
        }
        return Optional.ofNullable(paramValue)
                .filter(StringUtils::hasText)
                .map(this::stripBearer);
    }

    private String stripBearer(String token) {
        if (!StringUtils.hasText(token)) {
            return token;
        }
        String prefix = Optional.ofNullable(authProperties.getHeader()).orElse(IMConstant.BEARER_PREFIX);
        return token.startsWith(prefix) ? token.substring(prefix.length()).trim() : token.trim();
    }

    private void persistTokenRecord(String userId, String deviceId, String clientIp, long tokenVersion,
                                    long issuedAt, long accessExpiresIn, String accessToken, String refreshToken) {
        try {
            ImAuthTokenPo tokenPo = new ImAuthTokenPo()
                    .setId(UUID.randomUUID().toString().replace("-", ""))
                    .setUserId(userId)
                    .setDeviceId(deviceId)
                    .setClientIp(clientIp)
                    .setAccessTokenHash(DigestUtils.sha256Hex(accessToken))
                    .setRefreshTokenHash(DigestUtils.sha256Hex(refreshToken))
                    .setTokenVersion(tokenVersion)
                    .setIssuedAt(issuedAt)
                    .setAccessExpiresAt(issuedAt + TimeUnit.SECONDS.toMillis(accessExpiresIn))
                    .setUsed(0);
            authTokenDubboService.create(tokenPo);
        } catch (Exception e) {
            log.warn("令牌记录失败：{}", e.getMessage());
        }
    }

    private void cacheUserTokenMeta(String userId, String deviceId, long tokenVersion, long issuedAt,
                                    long accessExpiresIn, long refreshExpiresIn,
                                    String accessToken, String refreshToken) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("accessToken", accessToken);
        meta.put("refreshToken", refreshToken);
        meta.put("deviceId", deviceId);
        meta.put("tokenVersion", tokenVersion);
        meta.put("issuedAt", issuedAt);
        meta.put("accessExpiresAt", issuedAt + TimeUnit.SECONDS.toMillis(accessExpiresIn));
        meta.put("refreshExpiresAt", issuedAt + TimeUnit.SECONDS.toMillis(refreshExpiresIn));
        redisCache.set(USER_TOKEN_META_KEY + userId, meta, refreshExpiresIn, TimeUnit.SECONDS);
    }

    // ==================== 工具方法 ====================

    private String maskToken(String token) {
        if (!StringUtils.hasText(token) || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
