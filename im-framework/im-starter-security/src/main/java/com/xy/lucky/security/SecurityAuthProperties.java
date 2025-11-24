package com.xy.lucky.security;

import com.xy.lucky.core.constants.IMConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "security.auth")
public class SecurityAuthProperties {

    /**
     * 请求头
     */
    private String header = IMConstant.BEARER_PREFIX;

    /**
     * 忽略的url
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

}

