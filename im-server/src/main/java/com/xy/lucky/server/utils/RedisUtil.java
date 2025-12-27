package com.xy.lucky.server.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class RedisUtil {

    @Resource
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    /**
     * Get value by key
     * @param key key
     * @return Mono of value
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> get(String key) {
        return (Mono<T>) reactiveRedisTemplate.opsForValue().get(key);
    }

    /**
     * Batch get values
     * @param keys list of keys
     * @return Mono of list of values
     */
    public Mono<List<Object>> batchGet(List<String> keys) {
        return reactiveRedisTemplate.opsForValue().multiGet(keys);
    }
}
