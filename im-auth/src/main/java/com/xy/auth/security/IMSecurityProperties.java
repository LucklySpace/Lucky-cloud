package com.xy.auth.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;


/**
 * Security框架配置信息
 */

@Data
@RefreshScope
@ConfigurationProperties(
        prefix = "security"
)
public class IMSecurityProperties {

    /**
     * 过滤器忽略地址
     */
    private String[] ignore;

    /**
     * jwt 盐值
     */
    private String secret;

    /**
     * jwt token 过期时间  单位：min
     */
    private Integer expiration;


    private IMRSAKeyProperties IMRSAKeyProperties;
}