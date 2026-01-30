package com.xy.lucky.auth.service;

import java.util.Set;

/**
 * 令牌版本管理服务
 * <p>
 * 负责管理用户的令牌版本号，支持：
 * 1. 全局踢人（使所有设备的令牌失效）
 * 2. 设备踢人（使指定设备的令牌失效）
 * 3. 令牌版本校验
 *
 * @author dense
 */
public interface TokenVersionService {

    /**
     * 获取用户当前的全局令牌版本号
     *
     * @param userId 用户ID
     * @return 当前版本号，如果不存在则返回0
     */
    long getCurrentVersion(String userId);

    /**
     * 递增用户的全局令牌版本号（踢出所有设备）
     *
     * @param userId 用户ID
     * @return 新的版本号
     */
    long incrementVersion(String userId);

    /**
     * 校验令牌版本是否有效
     *
     * @param userId       用户ID
     * @param tokenVersion 令牌中的版本号
     * @return true表示版本有效，false表示令牌已失效
     */
    boolean isVersionValid(String userId, long tokenVersion);

    /**
     * 踢出用户的指定设备
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     */
    void kickDevice(String userId, String deviceId);

    /**
     * 踢出用户的所有设备
     *
     * @param userId 用户ID
     */
    void kickAllDevices(String userId);

    /**
     * 检查设备是否被踢出
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     * @return true表示设备已被踢出
     */
    boolean isDeviceKicked(String userId, String deviceId);

    /**
     * 注册活跃设备
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     */
    void registerDevice(String userId, String deviceId);

    /**
     * 获取用户所有活跃设备
     *
     * @param userId 用户ID
     * @return 活跃设备ID集合
     */
    Set<String> getActiveDevices(String userId);

    /**
     * 撤销令牌族（检测到重用攻击时调用）
     *
     * @param userId        用户ID
     * @param tokenFamilyId 令牌族ID
     */
    void revokeTokenFamily(String userId, String tokenFamilyId);

    /**
     * 检查令牌族是否被撤销
     *
     * @param userId        用户ID
     * @param tokenFamilyId 令牌族ID
     * @return true表示令牌族已被撤销
     */
    boolean isTokenFamilyRevoked(String userId, String tokenFamilyId);
}

