package com.xy.lucky.knowledge.service.impl;

import com.xy.lucky.knowledge.service.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockServiceImpl implements RedisLockService {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Boolean> acquireLock(String key, String value, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, ttl)
                .map(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        log.debug("Acquire lock success, key={}", key);
                        return true;
                    }
                    return false;
                });
    }

    @Override
    public Mono<Boolean> releaseLock(String key, String value) {
        return redisTemplate.opsForValue().get(key)
                .flatMap(current -> {
                    if (value.equals(current)) {
                        return redisTemplate.delete(key).map(deleted -> deleted > 0);
                    }
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }
}
