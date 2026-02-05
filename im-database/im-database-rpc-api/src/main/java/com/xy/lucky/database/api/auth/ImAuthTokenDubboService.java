package com.xy.lucky.database.api.auth;

import com.xy.lucky.domain.po.ImAuthTokenPo;

/**
 * 认证令牌持久化 Dubbo 服务接口
 */
public interface ImAuthTokenDubboService {

    /**
     * 保存令牌元信息
     */
    Boolean create(ImAuthTokenPo token);

    /**
     * 标记刷新令牌已使用
     */
    Boolean markUsedByRefreshHash(String refreshTokenHash);

    /**
     * 撤销访问令牌
     */
    Boolean revokeByAccessHash(String accessTokenHash, String reason);

    /**
     * 撤销刷新令牌
     */
    Boolean revokeByRefreshHash(String refreshTokenHash, String reason);
}