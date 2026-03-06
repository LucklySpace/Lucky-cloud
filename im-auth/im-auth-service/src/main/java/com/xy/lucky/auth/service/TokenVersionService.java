package com.xy.lucky.auth.service;

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
}

