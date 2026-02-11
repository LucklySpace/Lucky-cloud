package com.xy.lucky.connect.redis;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.utils.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 服务类
 * <p>
 * 使用 Spring Data Redis 标准方式封装 Redis 操作
 * 提供统一的 Redis 操作接口，自动管理连接和异常处理
 *
 * @author Lucky
 */
@Slf4j(topic = LogConstant.Redis)
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate,
                        StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // ======================== 字符串相关操作 ========================

    /**
     * 设置字符串值（无过期时间）
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis 设置字符串失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 设置字符串值（带过期时间）
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间（秒）
     */
    public void setEx(String key, String value, long seconds) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 设置字符串（带过期）失败: key={}, seconds={}", key, seconds, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 获取字符串值
     *
     * @param key 键
     * @return 值，如果不存在返回 null
     */
    public String get(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis 获取字符串失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 自增操作（用于计数器）
     *
     * @param key 键
     * @return 自增后的值
     */
    public long incr(String key) {
        try {
            return stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis 自增失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 判断 key 是否存在
     *
     * @param key 键
     * @return true 存在，false 不存在
     */
    public boolean exists(String key) {
        try {
            Boolean result = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis 判断 key 是否存在失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 设置过期时间（秒）
     *
     * @param key     键
     * @param seconds 过期时间（秒）
     */
    public void expire(String key, long seconds) {
        try {
            stringRedisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 设置过期时间失败: key={}, seconds={}", key, seconds, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 移除过期时间
     *
     * @param key 键
     */
    public void persist(String key) {
        try {
            stringRedisTemplate.persist(key);
        } catch (Exception e) {
            log.error("Redis 移除过期时间失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 如果 key 不存在则设置，并设置过期时间
     *
     * @param key            键
     * @param value          值
     * @param timeoutSeconds 过期时间（秒）
     * @return true 设置成功，false key 已存在
     */
    public boolean setnxWithTimeOut(String key, String value, long timeoutSeconds) {
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeoutSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis setnx 失败: key={}, timeoutSeconds={}", key, timeoutSeconds, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    // ======================== 批量操作 ========================

    /**
     * 批量新增 key，如果不存在则 set，并设置过期时间
     *
     * @param prefix        键前缀
     * @param objMap        对象映射
     * @param expireSeconds 过期时间（秒）
     */
    public void setnxBatch(String prefix, Map<String, Object> objMap, int expireSeconds) {
        try {
            Map<String, String> stringMap = objMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> prefix + entry.getKey(),
                            entry -> JacksonUtil.toJSONString(entry.getValue())));

            stringRedisTemplate.opsForValue().multiSet(stringMap);

            // 批量设置过期时间
            for (String key : stringMap.keySet()) {
                stringRedisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Redis 批量 setnx 失败: prefix={}, size={}", prefix, objMap.size(), e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 批量设置多个 key 的过期时间
     *
     * @param keyPrefix     键前缀（如 "user_route:"）
     * @param keys          用户ID集合
     * @param expireSeconds 过期时间（秒）
     */
    public void expireBatch(String keyPrefix, List<String> keys, long expireSeconds) {
        try {
            for (String key : keys) {
                stringRedisTemplate.expire(keyPrefix + key, expireSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Redis 批量设置过期时间失败: keyPrefix={}, size={}", keyPrefix, keys.size(), e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 批量删除多个 key
     *
     * @param keyPrefix 键前缀（如 "user_route:"）
     * @param keys      用户ID集合
     */
    public void deleteBatch(String keyPrefix, List<String> keys) {
        try {
            List<String> fullKeys = keys.stream()
                    .map(key -> keyPrefix + key)
                    .collect(Collectors.toList());
            stringRedisTemplate.delete(fullKeys);
        } catch (Exception e) {
            log.error("Redis 批量删除失败: keyPrefix={}, size={}", keyPrefix, keys.size(), e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    // ======================== 删除操作 ========================

    /**
     * 删除 key
     *
     * @param key 键
     * @return true 删除成功，false key 不存在
     */
    public boolean del(String key) {
        try {
            Boolean result = stringRedisTemplate.delete(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis 删除 key 失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    // ======================== List 列表操作 ========================

    /**
     * 向列表尾部插入值
     *
     * @param key   键
     * @param value 值
     * @return 列表长度
     */
    public long rpush(String key, String value) {
        try {
            return stringRedisTemplate.opsForList().rightPush(key, value);
        } catch (Exception e) {
            log.error("Redis rpush 失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    // ======================== Hash 哈希操作 ========================

    /**
     * 设置 HashMap 数据
     *
     * @param key 键
     * @param map 哈希映射
     */
    public void hmset(String key, Map<String, String> map) {
        try {
            stringRedisTemplate.opsForHash().putAll(key, map);
        } catch (Exception e) {
            log.error("Redis hmset 失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    // ======================== SortedSet 有序集合 ========================

    /**
     * 向有序集合添加成员
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     */
    public void zadd(String key, String value, double score) {
        try {
            stringRedisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            log.error("Redis zadd 失败: key={}, value={}, score={}", key, value, score, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 删除指定成员
     *
     * @param key    键
     * @param values 成员值
     * @return 删除的成员数量
     */
    public long zrem(String key, String... values) {
        try {
            return stringRedisTemplate.opsForZSet().remove(key, (Object[]) values);
        } catch (Exception e) {
            log.error("Redis zrem 失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 获取集合元素数量
     *
     * @param key 键
     * @return 元素数量
     */
    public long zcard(String key) {
        try {
            Long result = stringRedisTemplate.opsForZSet().zCard(key);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.error("Redis zcard 失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    // ======================== HyperLogLog ========================

    /**
     * 添加元素到 HyperLogLog
     *
     * @param key     键
     * @param element 元素
     */
    public void pfadd(String key, String element) {
        try {
            stringRedisTemplate.opsForHyperLogLog().add(key, element);
        } catch (Exception e) {
            log.error("Redis pfadd 失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 获取 HyperLogLog 估算数量
     *
     * @param key 键
     * @return 估算数量
     */
    public long pfcount(String key) {
        try {
            Long result = stringRedisTemplate.opsForHyperLogLog().size(key);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.error("Redis pfcount 失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    // ======================== 时间操作 ========================

//    /**
//     * 获取 Redis 服务器当前时间（秒）
//     *
//     * @return 时间戳（秒）
//     */
//    public long currentTimeSecond() {
//        try {
//            // 使用 Redis 的 TIME 命令
//            List<Object> result = stringRedisTemplate.execute(
//                    connection -> connection.time(),
//                    true);
//
//            if (result != null && result.size() >= 1) {
//                return Long.parseLong(String.valueOf(result.get(0)));
//            }
//            return System.currentTimeMillis() / 1000;
//        } catch (Exception e) {
//            log.error("Redis 获取服务器时间失败", e);
//            // 降级到本地时间
//            return System.currentTimeMillis() / 1000;
//        }
//    }

    // ======================== 对象操作（使用 JSON 序列化） ========================

    /**
     * 设置对象值（使用 JSON 序列化）
     *
     * @param key   键
     * @param value 对象值
     */
    public void setObject(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis 设置对象失败: key={}", key, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 设置对象值（带过期时间）
     *
     * @param key     键
     * @param value   对象值
     * @param seconds 过期时间（秒）
     */
    public void setObjectEx(String key, Object value, long seconds) {
        try {
            redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 设置对象（带过期）失败: key={}, seconds={}", key, seconds, e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 获取对象值
     *
     * @param key   键
     * @param clazz 对象类型
     * @param <T>   对象类型
     * @return 对象值，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return clazz.cast(value);
        } catch (Exception e) {
            log.error("Redis 获取对象失败: key={}, clazz={}", key, clazz.getName(), e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }
}
