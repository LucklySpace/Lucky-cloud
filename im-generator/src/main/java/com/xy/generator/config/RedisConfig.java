package com.xy.generator.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

/**
 * Redis配置类
 * 配置ReactiveRedisTemplate以支持响应式编程模型
 */
@EnableCaching
@Configuration
public class RedisConfig {

    /**
     * 创建ReactiveRedisTemplate实例，使用自定义的序列化方式
     *
     * @param factory Redis连接工厂
     * @param context 自定义的序列化上下文
     * @return ReactiveRedisTemplate实例
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            MyRedisSerializationContext context) {
        return new ReactiveRedisTemplate<>(factory, context);
    }
}