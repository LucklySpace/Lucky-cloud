package com.xy.lucky.auth.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class BloomFilterUtil {

    // 位数组长度
    private static final int SIZE = 1_000_000;

    // 多种哈希函数种子
    private static final int[] SEEDS = {7, 11, 13, 31, 37, 61};
    private static final String BLOOM_KEY = "username_bloom_filter";
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 添加元素
     */
    public void add(String username) {
        for (int seed : SEEDS) {
            int hash = hash(username, seed);
            redisTemplate.opsForValue().setBit(BLOOM_KEY, hash, true);
        }
    }

    /**
     * 判断可能存在
     */
    public boolean mightContain(String username) {
        for (int seed : SEEDS) {
            int hash = hash(username, seed);
            Boolean bit = redisTemplate.opsForValue().getBit(BLOOM_KEY, hash);
            if (bit == null || !bit) {
                return false; // 只要有一个 bit 为 false，则一定不存在
            }
        }
        return true; // 所有 bit 都是 true，则可能存在
    }

    /**
     * 哈希函数
     */
    private int hash(String value, int seed) {
        int result = 0;
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : data) {
            result = result * seed + b;
        }
        return Math.abs(result % SIZE);
    }

}
