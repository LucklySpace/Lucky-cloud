package com.xy.lucky.knowledge.service;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface RedisLockService {
    /**
     * 获取分布式锁
     */
    Mono<Boolean> acquireLock(String key, String value, Duration ttl);

    /**
     * 释放分布式锁
     */
    Mono<Boolean> releaseLock(String key, String value);
}
