package com.xy.lucky.gateway.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Component
public class MyRedisSerializationContext implements RedisSerializationContext {
    Jackson2JsonRedisSerializer<Object> Jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
    ObjectMapper mp = new ObjectMapper();
    private StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
    private GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

    public MyRedisSerializationContext() {
        mp.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mp.activateDefaultTyping(mp.getPolymorphicTypeValidator());
        Jackson2JsonRedisSerializer.serialize(mp);
    }

    //设置key的序列化方式
    @Override
    public SerializationPair getKeySerializationPair() {
        return SerializationPair.fromSerializer(stringRedisSerializer);
    }

    //设置value的序列化方式
    @Override
    public SerializationPair getValueSerializationPair() {
        return SerializationPair.fromSerializer(Jackson2JsonRedisSerializer);
    }

    //设置hashkey的序列化方式
    @Override
    public SerializationPair getHashKeySerializationPair() {
        return SerializationPair.fromSerializer(stringRedisSerializer);
    }

    //设置hashvalue的序列化方式
    @Override
    public SerializationPair getHashValueSerializationPair() {
        return SerializationPair.fromSerializer(Jackson2JsonRedisSerializer);
    }

    //设置spring字符串的序列化方式
    @Override
    public SerializationPair<String> getStringSerializationPair() {
        return SerializationPair.fromSerializer(stringRedisSerializer);
    }


}
