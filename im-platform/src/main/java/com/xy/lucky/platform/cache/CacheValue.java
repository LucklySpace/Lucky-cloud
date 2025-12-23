package com.xy.lucky.platform.cache;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 本地缓存值对象
 */
public class CacheValue {

    /**
     * 原始URL
     */
    public final String originalUrl;
    /**
     * 过期时间
     */
    public final LocalDateTime expireTime;
    /**
     * 是否启用
     */
    public final Boolean enabled;
    /**
     * 访问次数
     */
    public Integer visitCount;

    public CacheValue(String originalUrl, LocalDateTime expireTime, Boolean enabled, Integer visitCount) {
        this.originalUrl = originalUrl;
        this.expireTime = expireTime;
        this.enabled = enabled;
        this.visitCount = visitCount;
    }

    @SuppressWarnings("unchecked")
    public static CacheValue fromMap(Map<Object, Object> map) {
        String originalUrl = (String) map.getOrDefault("originalUrl", "");
        String expireEpoch = (String) map.getOrDefault("expireEpoch", "");
        LocalDateTime expireTime = null;
        if (StringUtils.hasText(expireEpoch)) {
            long epoch = Long.parseLong(expireEpoch);
            expireTime = LocalDateTime.ofEpochSecond(epoch, 0, java.time.ZoneOffset.UTC);
        }
        Boolean enabled = Boolean.parseBoolean(String.valueOf(map.getOrDefault("enabled", "true")));
        Integer visitCount = Integer.parseInt(String.valueOf(map.getOrDefault("visitCount", "0")));
        return new CacheValue(originalUrl, expireTime, enabled, visitCount);
    }
}