package com.xy.lucky.auth.service.impl;

import com.xy.lucky.auth.service.TokenVersionService;
import com.xy.lucky.auth.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 令牌版本管理服务实现
 * <p>
 * 基于 Redis 实现令牌版本控制和设备管理，支持：
 * - 全局踢人：递增版本号使所有令牌失效
 * - 设备踢人：将设备加入黑名单
 * - 令牌族撤销：检测到重用攻击时撤销整个会话链
 *
 * @author dense
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVersionServiceImpl implements TokenVersionService {

    /**
     * 用户全局令牌版本号 key: auth:version:{userId}
     */
    private static final String VERSION_KEY_PREFIX = "auth:version:";

    /**
     * 用户活跃设备集合 key: auth:devices:{userId}
     */
    private static final String DEVICES_KEY_PREFIX = "auth:devices:";

    /**
     * 被踢设备黑名单 key: auth:kicked:{userId}:{deviceId}
     */
    private static final String KICKED_DEVICE_PREFIX = "auth:kicked:";

    /**
     * 被撤销的令牌族 key: auth:revoked_family:{userId}:{familyId}
     */
    private static final String REVOKED_FAMILY_PREFIX = "auth:revoked_family:";

    /**
     * 设备黑名单过期时间（与 refresh token 过期时间一致）
     */
    private static final long KICKED_DEVICE_TTL_HOURS = 720;

    /**
     * 令牌族撤销记录过期时间
     */
    private static final long REVOKED_FAMILY_TTL_HOURS = 720;

    private final RedisCache redisCache;

    @Override
    public long getCurrentVersion(String userId) {
        String key = VERSION_KEY_PREFIX + userId;
        Long version = redisCache.get(key);
        return Optional.ofNullable(version).orElse(0L);
    }

    @Override
    public long incrementVersion(String userId) {
        String key = VERSION_KEY_PREFIX + userId;
        long newVersion = redisCache.incr(key, 1);
        log.info("用户 {} 令牌版本递增至 {}", userId, newVersion);
        return newVersion;
    }

    @Override
    public boolean isVersionValid(String userId, long tokenVersion) {
        long currentVersion = getCurrentVersion(userId);
        // 令牌版本必须大于等于当前版本才有效
        // 注意：这里使用 >= 而不是 ==，允许在版本递增前签发的令牌继续使用
        // 如果需要更严格的控制，可以改为 ==
        boolean valid = tokenVersion >= currentVersion;
        if (!valid) {
            log.debug("令牌版本校验失败：userId={}, tokenVersion={}, currentVersion={}",
                    userId, tokenVersion, currentVersion);
        }
        return valid;
    }

    @Override
    public void kickDevice(String userId, String deviceId) {
        String key = buildKickedDeviceKey(userId, deviceId);
        redisCache.set(key, System.currentTimeMillis(), KICKED_DEVICE_TTL_HOURS, TimeUnit.HOURS);

        // 从活跃设备列表移除
        String devicesKey = DEVICES_KEY_PREFIX + userId;
        redisCache.setRemove(devicesKey, deviceId);

        log.info("设备已被踢出：userId={}, deviceId={}", userId, deviceId);
    }

    @Override
    public void kickAllDevices(String userId) {
        // 1. 递增版本号，使所有令牌失效
        incrementVersion(userId);

        // 2. 清空活跃设备列表
        String devicesKey = DEVICES_KEY_PREFIX + userId;
        Set<String> devices = getActiveDevices(userId);
        if (devices != null && !devices.isEmpty()) {
            for (String deviceId : devices) {
                String kickedKey = buildKickedDeviceKey(userId, deviceId);
                redisCache.set(kickedKey, System.currentTimeMillis(), KICKED_DEVICE_TTL_HOURS, TimeUnit.HOURS);
            }
        }
        redisCache.del(devicesKey);

        log.info("用户 {} 所有设备已被踢出", userId);
    }

    @Override
    public boolean isDeviceKicked(String userId, String deviceId) {
        String key = buildKickedDeviceKey(userId, deviceId);
        return redisCache.hasKey(key);
    }

    @Override
    public void registerDevice(String userId, String deviceId) {
        String key = DEVICES_KEY_PREFIX + userId;
        redisCache.sSet(key, deviceId);
        // 设置设备列表过期时间（与 refresh token 最长有效期一致）
        redisCache.expire(key, TimeUnit.HOURS.toSeconds(KICKED_DEVICE_TTL_HOURS));
        log.debug("设备已注册：userId={}, deviceId={}", userId, deviceId);
    }

    @Override
    public Set<String> getActiveDevices(String userId) {
        String key = DEVICES_KEY_PREFIX + userId;
        Set<String> devices = redisCache.sGet(key);
        return devices != null ? devices : Collections.emptySet();
    }

    @Override
    public void revokeTokenFamily(String userId, String tokenFamilyId) {
        String key = buildRevokedFamilyKey(userId, tokenFamilyId);
        redisCache.set(key, System.currentTimeMillis(), REVOKED_FAMILY_TTL_HOURS, TimeUnit.HOURS);
        log.warn("令牌族已被撤销（可能检测到重用攻击）：userId={}, familyId={}", userId, tokenFamilyId);
    }

    @Override
    public boolean isTokenFamilyRevoked(String userId, String tokenFamilyId) {
        String key = buildRevokedFamilyKey(userId, tokenFamilyId);
        return redisCache.hasKey(key);
    }

    private String buildKickedDeviceKey(String userId, String deviceId) {
        return KICKED_DEVICE_PREFIX + userId + ":" + deviceId;
    }

    private String buildRevokedFamilyKey(String userId, String tokenFamilyId) {
        return REVOKED_FAMILY_PREFIX + userId + ":" + tokenFamilyId;
    }
}

