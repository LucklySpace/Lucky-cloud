package com.xy.lucky.message.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Redis 通用操作封装。
 * 统一处理常见的参数校验、过期时间保护和空结果兜底，避免业务层重复样板代码。
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取单个键值。
     *
     * @param key 键
     */
    public Object get(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 批量获取键值。
     * 使用 MGET 一次性拉取，结果顺序与 keys 保持一致。
     *
     * @param keys 键集合
     * @return 值
     */
    public List<Object> batchGet(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        return Objects.requireNonNullElse(values, Collections.emptyList());
    }

    /**
     * 设置键值并指定过期时间
     *
     * @param key           键
     * @param value         值
     * @param expireSeconds 过期时间（秒）
     */
    public void set(String key, Object value, long expireSeconds) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        long ttl = Math.max(1L, expireSeconds);
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
    }

    /**
     * 删除键
     *
     * @param key 键
     */
    public void del(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        redisTemplate.delete(key);
    }

    /**
     * 如果不存在则设置，并指定过期时间
     *
     * @param key           键
     * @param value         值
     * @param expireSeconds 过期时间（秒）
     * @return 是否设置成功
     */
    public boolean setIfAbsent(String key, Object value, long expireSeconds) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        long ttl = Math.max(1L, expireSeconds);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(ttl));
        return result != null && result;
    }
}
