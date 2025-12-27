package com.xy.lucky.server.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;

@EnableCaching  //开启缓存注解功能
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

//    @Value("${spring.data.redis.redisson.address}")
//    private String address;
//
//    @Bean(destroyMethod = "shutdown")
//    public RedissonClient redisson() throws IOException {
//        return Redisson.create(
//                Config.fromYAML(new ClassPathResource(address).getInputStream()));
//    }

    @Value("${spring.data.redis.redisson.address}")
    private String address;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        Config config = new Config();

        // 普通单节点（非 TLS）
        config.useSingleServer()
                .setAddress(address)
                // 如果你的 Redis 仅需密码（老方式），只需 setPassword：
                .setDatabase(0)
                .setTimeout(3000);

        if (StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer
                = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig();
        configuration = configuration.serializeValuesWith
                        (RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer))
                .entryTtl(Duration.ofDays(1));
        return configuration;
    }


    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> valueSerializer = getSerializer();
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        RedisSerializationContext<String, Object> context = builder.value(valueSerializer).hashValue(valueSerializer).build();
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

    public Jackson2JsonRedisSerializer<Object> getSerializer() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);

//        om.addMixIn(Object.class, ObjectMixin.class);
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }


}