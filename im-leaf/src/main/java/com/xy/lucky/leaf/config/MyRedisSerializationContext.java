package com.xy.lucky.leaf.config;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * 自定义Redis序列化上下文
 * 配置Redis键值的序列化方式，确保数据在Redis中以可读的JSON格式存储
 */
@Component
public class MyRedisSerializationContext implements RedisSerializationContext {

    private final StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
    private final GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer =
            new GenericJackson2JsonRedisSerializer();

    /**
     * 设置key的序列化方式
     *
     * @return 序列化对
     */
    @Override
    public SerializationPair getKeySerializationPair() {
        return SerializationPair.fromSerializer(stringRedisSerializer);
    }

    /**
     * 设置value的序列化方式
     *
     * @return 序列化对
     */
    @Override
    public SerializationPair getValueSerializationPair() {
        return SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer);
    }

    /**
     * 设置hash key的序列化方式
     *
     * @return 序列化对
     */
    @Override
    public SerializationPair getHashKeySerializationPair() {
        return SerializationPair.fromSerializer(stringRedisSerializer);
    }

    /**
     * 设置hash value的序列化方式
     *
     * @return 序列化对
     */
    @Override
    public SerializationPair getHashValueSerializationPair() {
        return SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer);
    }

    /**
     * 设置Spring字符串的序列化方式
     *
     * @return 序列化对
     */
    @Override
    public SerializationPair<String> getStringSerializationPair() {
        return SerializationPair.fromSerializer(stringRedisSerializer);
    }
}