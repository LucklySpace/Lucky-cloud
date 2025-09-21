package com.xy.im.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableCaching  //开启缓存注解功能
@Configuration
public class RedisConfig {

    /**
     * 创建模板对象，使用自定义的序列化方式
     *
     * @param factory
     * @param context 我们手动指定的序列化方式
     * @return
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory, MyRedisSerializationContext context) {
        return new ReactiveRedisTemplate(factory, context);
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置objectMapper:转换java对象的时候使用
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置key/value值的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }


//    @Bean
//    public RedisCacheConfiguration redisCacheConfiguration() {
//        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer
//                = new GenericJackson2JsonRedisSerializer();
//        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig();
//        configuration = configuration.serializeValuesWith
//                        (RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer))
//                .entryTtl(Duration.ofDays(1));
//        return configuration;
//    }
//
//    /**
//     * retemplate相关配置
//     *
//     * @param factory
//     * @return
//     */
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//
//        //我们为了开发方便，一般直接使用<String, Object>
//        RedisTemplate<String, Object> template = new RedisTemplate<String,Object>();
//        template.setConnectionFactory(factory);
//
//        //Json序列化配置
//        Jackson2JsonRedisSerializer<Object> Jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
//        ObjectMapper mp = new ObjectMapper();
//        mp.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        mp.activateDefaultTyping(mp.getPolymorphicTypeValidator());
//        Jackson2JsonRedisSerializer.serialize(mp);
//
//        // Spring的序列化
//        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
//        // key采用String的序列化方式
//        template.setKeySerializer(stringRedisSerializer);
//        // hash的key也采用string的序列化方式
//        template.setHashKeySerializer(stringRedisSerializer);
//        // value的序列化方式采用的是jackson
//        template.setValueSerializer(Jackson2JsonRedisSerializer);
//        // hash的value序列化方式采用jackson
//        template.setHashKeySerializer(Jackson2JsonRedisSerializer);
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public RedisTemplate<String, Long> longRedisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Long> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
//        template.afterPropertiesSet();
//        return template;
//    }


}