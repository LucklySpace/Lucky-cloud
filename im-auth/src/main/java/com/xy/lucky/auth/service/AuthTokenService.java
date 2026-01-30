package com.xy.lucky.auth.service;

import com.xy.lucky.auth.domain.AuthTokenPair;

import java.util.Optional;

/**
 * 令牌管理服务接口，负责 Token 的签发、刷新、校验与撤销
 */
public interface AuthTokenService {

    /**
     * 为用户签发一对新的访问令牌和刷新令牌
     *
     * @param userId   用户 ID
     * @param deviceId 设备 ID
     * @param clientIp 客户端 IP
     * @return 令牌对
     */
    AuthTokenPair issueTokens(String userId, String deviceId, String clientIp);

    /**
     * 使用刷新令牌换取新的令牌对，并可选地撤销旧的刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @param clientIp     客户端 IP
     * @param deviceId     设备 ID
     * @return 新的令牌对
     */
    AuthTokenPair refreshTokens(String refreshToken, String clientIp, String deviceId);

    /**
     * 撤销指定的令牌（将其加入黑名单并从缓存移除）
     *
     * @param accessToken  待撤销的访问令牌
     * @param refreshToken 待撤销的刷新令牌
     */
    void revokeTokens(String accessToken, String refreshToken);

    /**
     * 从请求头或参数中解析并规范化访问令牌
     *
     * @param headerValue 原始请求头值
     * @param paramValue  原始参数值
     * @return 规范化后的令牌字符串
     */
    Optional<String> resolveAccessToken(String headerValue, String paramValue);

    /**
     * 从请求头或参数中解析并规范化刷新令牌
     *
     * @param headerValue 原始请求头值
     * @param paramValue  原始参数值
     * @return 规范化后的令牌字符串
     */
    Optional<String> resolveRefreshToken(String headerValue, String paramValue);
}

