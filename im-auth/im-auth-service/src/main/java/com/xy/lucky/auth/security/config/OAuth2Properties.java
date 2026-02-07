package com.xy.lucky.auth.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "security.oauth2")
public class OAuth2Properties {

    /**
     * 授权码有效期（秒）
     */
    private long authorizationCodeTtlSeconds = 300;

    /**
     * 是否强制 PKCE（默认开启）
     */
    private boolean pkceRequired = true;

    /**
     * 客户端配置列表
     */
    private List<Client> clients = new ArrayList<>();

    @Data
    public static class Client {
        private String clientId;
        private List<String> redirectUris = new ArrayList<>();
        private List<String> scopes = new ArrayList<>();
        private boolean requirePkce = true;
    }
}

