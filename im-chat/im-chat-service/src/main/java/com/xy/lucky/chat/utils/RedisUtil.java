package com.xy.lucky.chat.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取
     *
     * @param key   键
     */
    @SuppressWarnings("unchecked")
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 批量获取
     *
     * @param keys 键集合
     * @return 值
     */
    public List<Object> batchGet(List<String> keys) {
        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.get(key.getBytes());
            }
            return null;
        });
    }

    /**
     * 设置键值并指定过期时间
     *
     * @param key           键
     * @param value         值
     * @param expireSeconds 过期时间（秒）
     */
    public void set(String key, Object value, long expireSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
    }

    /**
     * 删除键
     *
     * @param key 键
     */
    public void del(String key) {
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
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(expireSeconds));
        return result != null && result;
    }
}
