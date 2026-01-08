package com.xy.lucky.server.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Get value by key
     * @param key key
     * @return Mono of value
     */
    @SuppressWarnings("unchecked")
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Batch get values
     * @param keys list of keys
     * @return Mono of list of values
     */
    public List<Object> batchGet(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }
}
