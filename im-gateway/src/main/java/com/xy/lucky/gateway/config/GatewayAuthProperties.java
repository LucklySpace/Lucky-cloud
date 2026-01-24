package com.xy.lucky.gateway.config;

import com.xy.lucky.core.constants.IMConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关安全与认证配置属性
 */
@Data
@ConfigurationProperties(prefix = "lucky.gateway.security")
public class GatewayAuthProperties {

    /**
     * 是否启用网关安全校验
     */
    private boolean enabled = true;

    /**
     * 白名单路径（不进行任何安全校验）
     */
    private List<String> ignore = new ArrayList<>();

    /**
     * 认证相关配置
     */
    private Auth auth = new Auth();

    /**
     * IP 频控与黑名单配置
     */
    private IpGuard ipGuard = new IpGuard();

    /**
     * 接口签名验证配置
     */
    private ApiSign sign = new ApiSign();

    // 兼容旧命名或快捷访问
    @Deprecated
    public Auth getAuth() {
        return auth;
    }

    @Data
    public static class Auth {
        private boolean enabled = true;
        private String header = IMConstant.AUTH_TOKEN_HEADER;
        private String bearerPrefix = IMConstant.BEARER_PREFIX;
        private String accessTokenParam = IMConstant.ACCESS_TOKEN_PARAM;
        private String userHeader = "X-User-Id";

        /**
         * 令牌黑名单校验（对接 Redis）
         */
        private boolean checkBlacklistEnabled = true;
        private String blacklistKeyPrefix = "auth:token:blacklist:";

        /**
         * 防重放保护
         */
        private boolean replayProtectionEnabled = true;
        private String nonceHeader = "X-Nonce";
        private String timestampHeader = "X-Timestamp";
        private long nonceTtlSeconds = 300;
        private long timestampWindowSeconds = 300;
    }

    @Data
    public static class IpGuard {
        private boolean enabled = true;
        private long windowSeconds = 5;
        private long maxRequests = 20;
        private long banSeconds = 600;

        /**
         * 布隆过滤器配置（用于超大规模黑名单快速过滤）
         */
        private Bloom bloom = new Bloom();

        @Data
        public static class Bloom {
            private boolean enabled = true;
            private double errorRate = 0.0001;
            private long capacity = 1000000;
            private long ttlHours = 24;
        }
    }

    @Data
    public static class ApiSign {
        private boolean enabled = false;
        private long expireTimeSeconds = 300;
        private String secretKeyPrefix = "auth:sign:secret:";
    }
}

