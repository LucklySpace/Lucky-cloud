package com.xy.lucky.platform.shortlink;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 短链配置
 * - domain：外部访问域名（如 https://s.example.com）
 * - apiVersion：用于拼接短链路径中的 {version}，默认 v1
 * - cacheCapacity：解析热点缓存容量
 * - deterministicLength：确定性短码长度（Murmur 截断）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "shortlink")
public class ShortLinkProperties {

    /**
     * 短链域名
     */
    private String domain = "";

    /**
     * API 版本
     * - 默认 v1
     * - 访问短链时，会拼接成 {domain}/{apiVersion}/{短链码}
     */
    private String apiVersion = "v1";

    /**
     * 解析热点缓存容量
     * - 默认 100_000
     */
    private int cacheCapacity = 100_000;

    /**
     * 缓存 TTL 时间  单位: 秒
     * 默认 7 天
     */
    private Long CacheTtlSeconds = 60 * 60 * 24 * 7L;

    /**
     * 确定性短码长度
     * - 默认 7
     */
    private int deterministicLength = 7;

    /**
     * Redis key 模板
     */
    private String KEY_CODE = "im-lucky:short:code:%s";

    /**
     * 布隆过滤器 key
     */
    private String BLOOM_KEY = "im-lucky:short:bloom:url";

    /**
     * 布隆过滤器大小 10亿
     */
    private int BLOOM_SIZE = 1_000_000_000;

    /**
     * 布隆过滤器种子
     */
    private int[] BLOOM_SEEDS = {7, 11, 13, 31, 37, 61, 89, 97, 101};
}

