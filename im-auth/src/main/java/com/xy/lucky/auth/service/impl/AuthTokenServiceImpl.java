package com.xy.lucky.auth.service.impl;

import com.xy.lucky.api.auth.ImAuthTokenDubboService;
import com.xy.lucky.auth.domain.AuthRefreshToken;
import com.xy.lucky.auth.domain.AuthTokenPair;
import com.xy.lucky.auth.service.AuthTokenService;
import com.xy.lucky.auth.service.TokenVersionService;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.domain.po.ImAuthTokenPo;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.SecurityAuthProperties;
import com.xy.lucky.security.exception.AuthenticationFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 令牌管理服务实现
 * <p>
 * 实现令牌签发、刷新、撤销逻辑
 * <ul>
 *   <li>令牌版本控制 - 支持全局踢人和设备踢人</li>
 *   <li>令牌族追踪 - 同一会话的令牌共享族ID</li>
 *   <li>重用攻击检测 - 检测到旧令牌被重用时撤销整个令牌族</li>
 *   <li>设备绑定校验 - 刷新时验证设备一致性</li>
 *   <li>绝对过期时间 - 防止无限刷新</li>
 * </ul>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    // ==================== Redis Key 前缀定义 ====================

    /**
     * 访问令牌 -> 用户ID 映射
     */
    private static final String ACCESS_TOKEN_KEY = "auth:token:access:";

    /**
     * 刷新令牌 -> 令牌元数据 映射
     */
    private static final String REFRESH_TOKEN_KEY = "auth:token:refresh:";

    /**
     * 令牌黑名单
     */
    private static final String BLACKLIST_KEY = "auth:token:blacklist:";

    /**
     * 已使用的刷新令牌记录（用于重用检测）
     */
    private static final String USED_REFRESH_TOKEN_KEY = "auth:token:used:";

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

        // 获取配置的过期时间
        int accessTtlMinutes = Optional.ofNullable(authProperties.getExpiration()).orElse(30);
        int refreshTtlHours = Optional.ofNullable(authProperties.getRefreshExpiration()).orElse(720);

        // 获取当前令牌版本
        long tokenVersion = tokenVersionService.getCurrentVersion(userId);

        // 生成新的令牌族ID（首次登录时创建新族）
        String tokenFamilyId = generateTokenFamilyId();

        // 计算绝对过期时间（refresh token 最长有效期）
        long absoluteExpiresAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(refreshTtlHours);

        return doIssueTokens(userId, deviceId, clientIp, tokenVersion, tokenFamilyId, 0, absoluteExpiresAt);
    }

    /**
     * 内部令牌签发方法，支持刷新时复用令牌族
     */
    private AuthTokenPair doIssueTokens(String userId, String deviceId, String clientIp,
                                        long tokenVersion, String tokenFamilyId,
                                        int sequenceNumber, long absoluteExpiresAt) {

        int accessTtlMinutes = Optional.ofNullable(authProperties.getExpiration()).orElse(30);
        int refreshTtlHours = Optional.ofNullable(authProperties.getRefreshExpiration()).orElse(720);

        // 计算过期时间（秒）
        long accessExpiresIn = TimeUnit.MINUTES.toSeconds(accessTtlMinutes);
        long refreshExpiresIn = TimeUnit.HOURS.toSeconds(refreshTtlHours);

        // 检查绝对过期时间，防止无限刷新
        long now = System.currentTimeMillis();
        long maxRefreshExpiresAt = now + TimeUnit.SECONDS.toMillis(refreshExpiresIn);
        if (maxRefreshExpiresAt > absoluteExpiresAt) {
            // 调整刷新令牌过期时间，不超过绝对过期时间
            refreshExpiresIn = Math.max(0, (absoluteExpiresAt - now) / 1000);
            if (refreshExpiresIn <= 0) {
                log.warn("令牌已达绝对过期时间，需要重新登录：userId={}", userId);
                throw new AuthenticationFailException(ResultCode.TOKEN_EXPIRED_NEED_RELOGIN);
            }
        }

        // 生成访问令牌（JWT，包含版本号）
        String accessToken = JwtUtil.createToken(userId, tokenVersion, accessTtlMinutes, ChronoUnit.MINUTES);

        // 生成不透明刷新令牌
        String refreshToken = generateRefreshToken();

        // 构建刷新令牌元数据
        AuthRefreshToken refreshTokenMeta = AuthRefreshToken.builder()
                .userId(userId)
                .deviceId(deviceId)
                .clientIp(clientIp)
                .issuedAt(now)
                .tokenVersion(tokenVersion)
                .tokenFamilyId(tokenFamilyId)
                .sequenceNumber(sequenceNumber)
                .used(false)
                .absoluteExpiresAt(absoluteExpiresAt)
                .build();

        // 存储访问令牌映射
        redisCache.set(ACCESS_TOKEN_KEY + accessToken, userId, accessExpiresIn, TimeUnit.SECONDS);

        // 存储刷新令牌元数据
        redisCache.set(REFRESH_TOKEN_KEY + refreshToken, refreshTokenMeta, refreshExpiresIn, TimeUnit.SECONDS);

        // 持久化令牌信息（存储哈希，避免泄露原始令牌）
        try {
            ImAuthTokenPo tokenPo = new ImAuthTokenPo()
                    .setId(UUID.randomUUID().toString().replace("-", ""))
                    .setUserId(userId)
                    .setDeviceId(deviceId)
                    .setClientIp(clientIp)
                    .setAccessTokenHash(DigestUtils.sha256Hex(accessToken))
                    .setRefreshTokenHash(DigestUtils.sha256Hex(refreshToken))
                    .setTokenVersion(tokenVersion)
                    .setTokenFamilyId(tokenFamilyId)
                    .setSequenceNumber(sequenceNumber)
                    .setIssuedAt(now)
                    .setAccessExpiresAt(now + TimeUnit.SECONDS.toMillis(accessExpiresIn))
                    .setAbsoluteExpiresAt(absoluteExpiresAt)
                    .setUsed(0);
            authTokenDubboService.create(tokenPo);
        } catch (Exception e) {
            log.warn("令牌持久化失败：{}", e.getMessage());
        }

        // 注册活跃设备
        if (StringUtils.hasText(deviceId)) {
            tokenVersionService.registerDevice(userId, deviceId);
        }

        log.info("令牌签发成功：userId={}, deviceId={}, version={}, familyId={}, seq={}",
                userId, deviceId, tokenVersion, tokenFamilyId, sequenceNumber);

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
        // 1. 基础校验
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("刷新令牌为空");
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        // 2. 检查黑名单
        if (isTokenBlacklisted(refreshToken)) {
            log.warn("刷新令牌已被撤销：{}", maskToken(refreshToken));
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
        }

        // 3. 获取刷新令牌元数据
        String refreshKey = REFRESH_TOKEN_KEY + refreshToken;
        AuthRefreshToken tokenMeta = redisCache.get(refreshKey);
        if (tokenMeta == null) {
            log.warn("刷新令牌不存在或已过期：{}", maskToken(refreshToken));
            throw new AuthenticationFailException(ResultCode.TOKEN_EXPIRED);
        }

        String userId = tokenMeta.getUserId();
        String tokenFamilyId = tokenMeta.getTokenFamilyId();

        // 4. 检测令牌重用攻击
        if (tokenMeta.isUsed()) {
            // 该令牌已被使用过，可能是重用攻击！
            log.error("检测到令牌重用攻击！userId={}, familyId={}, token={}",
                    userId, tokenFamilyId, maskToken(refreshToken));
            // 撤销整个令牌族
            tokenVersionService.revokeTokenFamily(userId, tokenFamilyId);
            // 踢出该设备
            if (StringUtils.hasText(tokenMeta.getDeviceId())) {
                tokenVersionService.kickDevice(userId, tokenMeta.getDeviceId());
            }
            throw new AuthenticationFailException(ResultCode.TOKEN_REUSE_DETECTED);
        }

        // 5. 检查令牌族是否已被撤销
        if (tokenVersionService.isTokenFamilyRevoked(userId, tokenFamilyId)) {
            log.warn("令牌族已被撤销：userId={}, familyId={}", userId, tokenFamilyId);
            throw new AuthenticationFailException(ResultCode.TOKEN_FAMILY_REVOKED);
        }

        // 6. 校验令牌版本
        if (!tokenVersionService.isVersionValid(userId, tokenMeta.getTokenVersion())) {
            log.warn("令牌版本已失效：userId={}, tokenVersion={}", userId, tokenMeta.getTokenVersion());
            throw new AuthenticationFailException(ResultCode.TOKEN_VERSION_INVALID);
        }

        // 7. 校验设备绑定
        if (!validateDeviceBinding(tokenMeta, deviceId)) {
            log.warn("设备绑定校验失败：storedDevice={}, requestDevice={}",
                    tokenMeta.getDeviceId(), deviceId);
            throw new AuthenticationFailException(ResultCode.DEVICE_MISMATCH);
        }

        // 8. 检查设备是否被踢出
        String boundDeviceId = StringUtils.hasText(deviceId) ? deviceId : tokenMeta.getDeviceId();
        if (StringUtils.hasText(boundDeviceId) && tokenVersionService.isDeviceKicked(userId, boundDeviceId)) {
            log.warn("设备已被踢出：userId={}, deviceId={}", userId, boundDeviceId);
            throw new AuthenticationFailException(ResultCode.DEVICE_KICKED);
        }

        // 9. 标记当前令牌为已使用（防止重用）
        markRefreshTokenAsUsed(refreshToken, tokenMeta);

        // 10. 签发新令牌对（继承令牌族，序号递增）
        AuthTokenPair newPair = doIssueTokens(
                userId,
                boundDeviceId,
                clientIp,
                tokenVersionService.getCurrentVersion(userId),  // 使用最新版本
                tokenFamilyId,
                tokenMeta.getSequenceNumber() + 1,
                tokenMeta.getAbsoluteExpiresAt()
        );

        // 11. 处理令牌轮换策略
        handleTokenRotation(refreshToken, refreshKey);

        log.info("令牌刷新成功：userId={}, familyId={}, newSeq={}",
                userId, tokenFamilyId, tokenMeta.getSequenceNumber() + 1);

        return newPair;
    }

    /**
     * 校验设备绑定
     */
    private boolean validateDeviceBinding(AuthRefreshToken tokenMeta, String requestDeviceId) {
        String storedDeviceId = tokenMeta.getDeviceId();

        // 如果原令牌没有绑定设备，则允许
        if (!StringUtils.hasText(storedDeviceId)) {
            return true;
        }

        // 如果请求没有提供设备ID，但原令牌绑定了设备，严格模式下应该拒绝
        // 这里采用宽松策略：允许（但会记录警告）
        if (!StringUtils.hasText(requestDeviceId)) {
            log.debug("刷新请求未提供设备ID，原令牌绑定设备：{}", storedDeviceId);
            return true;
        }

        // 设备ID必须匹配
        return storedDeviceId.equals(requestDeviceId);
    }

    /**
     * 标记刷新令牌为已使用
     */
    private void markRefreshTokenAsUsed(String refreshToken, AuthRefreshToken tokenMeta) {
        tokenMeta.setUsed(true);
        String refreshKey = REFRESH_TOKEN_KEY + refreshToken;
        long ttl = redisCache.getExpire(refreshKey);
        if (ttl > 0) {
            redisCache.set(refreshKey, tokenMeta, ttl, TimeUnit.SECONDS);
        }

        // 同时记录到已使用令牌集合（用于快速检测）
        String usedKey = USED_REFRESH_TOKEN_KEY + refreshToken;
        redisCache.set(usedKey, System.currentTimeMillis(), ttl, TimeUnit.SECONDS);

        // 数据库标记刷新令牌已使用
        try {
            authTokenDubboService.markUsedByRefreshHash(DigestUtils.sha256Hex(refreshToken));
        } catch (Exception e) {
            log.warn("标记刷新令牌已使用失败：{}", e.getMessage());
        }
    }

    /**
     * 处理令牌轮换策略
     */
    private void handleTokenRotation(String oldRefreshToken, String refreshKey) {
        boolean reuseRefreshToken = Boolean.TRUE.equals(authProperties.getReuseRefreshTokens());

        if (!reuseRefreshToken) {
            // 不复用刷新令牌：将旧令牌加入黑名单并删除
            blacklistToken(oldRefreshToken, true);
            redisCache.del(refreshKey);
            try {
                authTokenDubboService.revokeByRefreshHash(DigestUtils.sha256Hex(oldRefreshToken), "rotation");
            } catch (Exception e) {
                log.warn("刷新令牌轮换撤销持久化失败：{}", e.getMessage());
            }
        }
        // 如果配置为复用刷新令牌，则保留旧令牌（但已标记为used）
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
        redisCache.del(ACCESS_TOKEN_KEY + accessToken);

        try {
            authTokenDubboService.revokeByAccessHash(DigestUtils.sha256Hex(accessToken), "manual_logout");
        } catch (Exception e) {
            log.warn("访问令牌撤销持久化失败：{}", e.getMessage());
        }
        log.debug("访问令牌已撤销：{}", maskToken(accessToken));
    }

    /**
     * 撤销刷新令牌
     */
    private void revokeRefreshToken(String refreshToken) {
        String refreshKey = REFRESH_TOKEN_KEY + refreshToken;
        long ttlSeconds = redisCache.getExpire(refreshKey);
        if (ttlSeconds <= 0) {
            ttlSeconds = TimeUnit.HOURS.toSeconds(
                    Optional.ofNullable(authProperties.getRefreshExpiration()).orElse(720));
        }

        redisCache.set(BLACKLIST_KEY + refreshToken, System.currentTimeMillis(), ttlSeconds, TimeUnit.SECONDS);
        redisCache.del(refreshKey);

        try {
            authTokenDubboService.revokeByRefreshHash(DigestUtils.sha256Hex(refreshToken), "manual_logout");
        } catch (Exception e) {
            log.warn("刷新令牌撤销持久化失败：{}", e.getMessage());
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

    // ==================== 工具方法 ====================

    /**
     * 生成令牌族ID
     */
    private String generateTokenFamilyId() {
        return "fam_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 生成刷新令牌
     */
    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 掩码令牌用于日志输出
     */
    private String maskToken(String token) {
        if (!StringUtils.hasText(token) || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
