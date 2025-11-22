package com.xy.lucky.gateway.utils;

import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis工具类
 */

public class RedisUtil {

    //@Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 根据key获取值
     *
     * @param key 键
     * @return value 值
     */
    public String get(String key) {
        BoundValueOperations<String, String> ops = redisTemplate.boundValueOps(key);
        String value = ops.get();
        return value;
    }


    /**
     * 保存键值对
     *
     * @param key   键
     * @param value 值
     */
    public void save(String key, String value) {
        BoundValueOperations<String, String> ops = redisTemplate.boundValueOps(key);
        ops.set(value);
    }

    /**
     * 设置键值对过期
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void saveExpired(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.boundValueOps(key).setIfAbsent(value, timeout, unit);
    }

    /**
     * 根据key删除键值对
     *
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 清空redis
     *
     * @param
     */
    public void deleteAll() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }


    public long getKeysCount(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys.size();
    }


    public Set<String> fuzzyQuery(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 使用pipelined批量存储
     *
     * @param: map  数据
     * @param: seconds  过期时间  设置-1 永不过期
     * @return: void
     */
    public void executePipelined(Map<String, String> map, long seconds) {

        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        redisTemplate.executePipelined((RedisCallback<String>) connection -> {
            map.forEach((key, value) -> {
                if (seconds == -1) { // 没有指定过期时间，则设置为不过期
                    connection.set(serializer.serialize(key), serializer.serialize(value), Expiration.persistent(), RedisStringCommands.SetOption.UPSERT);
                } else {
                    connection.set(serializer.serialize(key), serializer.serialize(value), Expiration.seconds(seconds), RedisStringCommands.SetOption.UPSERT);
                }
            });
            return null;
        }, serializer);

    }

    // 获取redis当前键的失效时间
    public Long getExpire(String key) {
        if (null == key) {
            //throw new BusinessException(BaseResponseCode.DATA_ERROR.getCode(), "key or TomeUnit 不能为空");
        }
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public void save(String key, String value, int expire, TimeUnit timeUnit) {
        BoundValueOperations<String, String> ops = redisTemplate.boundValueOps(key);
        ops.set(value, expire, timeUnit);
    }

    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }
}
