package com.xy.lucky.oss.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OSS 配置属性
 * 支持单实例与多提供者配置：
 * - providers：多对象存储提供者集合
 * - defaultProvider：默认提供者标识
 * - bucketProviderByCode：按桶名后缀 code 路由到指定提供者
 * 同时提供连接参数、超时、path-style 等通用设置
 */
@Data
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    private String endpoint;
    private String region;
    private Boolean pathStyleAccess = false;
    private String accessKey;
    private String secretKey;
    private Integer maxConnections = 100;
    private Duration connectionTimeout = Duration.ofSeconds(60);
    private Duration socketTimeout = Duration.ofSeconds(60);
    private Integer connectionPoolMaxIdle = 10;
    private Duration connectionPoolKeepAlive = Duration.ofMinutes(5);
    private Integer presignedUrlExpiry = 7 * 24 * 60 * 60;
    private String bucketName;
    private Boolean createThumbnail = Boolean.FALSE;
    private Boolean createWatermark = Boolean.FALSE;
    private Boolean compress = Boolean.FALSE;
    private Boolean checkFile = Boolean.FALSE;
    private Boolean calculateDuration = Boolean.TRUE;
    private Map<String, Provider> providers = new LinkedHashMap<>();
    private String defaultProvider;
    private Map<String, String> bucketProviderByCode = new LinkedHashMap<>();

    public boolean isValid() {
        return providers != null && !providers.isEmpty() && defaultProvider != null && !defaultProvider.isEmpty();
    }

    public String getAccessKey() {
        return accessKey == null ? null : accessKey.trim();
    }

    public String getSecretKey() {
        return secretKey == null ? null : secretKey.trim();
    }

    public String getEndpoint() {
        return endpoint == null ? null : endpoint.trim();
    }

    public String getRegion() {
        return region == null ? null : region.trim();
    }

    /**
     * 单个提供者配置
     * 包含 endpoint、region、accessKey、secretKey、pathStyleAccess 等
     * 可选 presignedUrlExpiry（单位：秒）
     */
    @Data
    public static class Provider {
        private String name;
        private String endpoint;
        private String region;
        private String accessKey;
        private String secretKey;
        private Boolean pathStyleAccess = false;
        private Integer presignedUrlExpiry;
    }
}
