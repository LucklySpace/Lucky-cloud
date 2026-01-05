package com.xy.lucky.gateway.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableCaching  //开启缓存注解功能
@Configuration
public class RedisConfig {

    /**
     * 创建模板对象，使用自定义的序列化方式
     *
     * @param factory
     * @return
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = getSerializer();
        
        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(new StringRedisSerializer())
                .value(jacksonSerializer)
                .hashKey(new StringRedisSerializer())
                .hashValue(jacksonSerializer)
                .build();
                
        return new ReactiveRedisTemplate<>(factory, context);
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 配置 Jackson 序列化器
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = getSerializer();

        // 设置 key 和 value 的序列化规则
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jacksonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.redisson.address:}") String address,
            @Value("${spring.data.redis.password:}") String password) {
        if (!org.springframework.util.StringUtils.hasText(address)) {
            return Redisson.create();
        }

        Config config = new Config();
        config.useSingleServer().setAddress(address);
        if (org.springframework.util.StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }

    public Jackson2JsonRedisSerializer<Object> getSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(), 
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        // 添加类型信息以防止反序列化时出现 LinkedHashMap 问题
        objectMapper.addMixIn(Object.class, ObjectMixin.class);
        
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
    
    // 用于添加类型信息的 Mixin
    public abstract static class ObjectMixin {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
        public abstract Object getValue();
    }
}
