package com.xy.lucky.connect.redis;


import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.Value;
import com.xy.lucky.spring.core.ProxyType;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * redis工具类
 *
 * @author lc
 */
@Slf4j
@Component(proxy = ProxyType.NONE)// 注册为自定义 Spring 框架的组件
public class RedisTemplate {

    private JedisPool jedisPool;

    // 以下配置通过自定义 @Value 注解注入
    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private Integer port;

    @Value("${redis.password}")
    private String password;

    @Value("${redis.timeout}")
    private Integer timeout = 10000; // 默认超时为10秒

    /**
     * 初始化 Redis 连接池，使用自定义框架中的 @PostConstruct 注解标识初始化方法
     */
    @PostConstruct
    public void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(100);              // 最大连接数
        config.setMaxIdle(10);                // 最大空闲连接
        config.setMaxWaitMillis(timeout);     // 最大等待时间
        config.setTestOnBorrow(true);         // 获取连接前是否测试有效性

        // 创建连接池（是否使用密码）
        if (StringUtils.hasText(password)) {
            this.jedisPool = new JedisPool(config, host, port, timeout, password);
        } else {
            this.jedisPool = new JedisPool(config, host, port, timeout);
        }

        log.info("RedisTemplate 初始化成功，host={}, port={}", host, port);
    }

    // ======================== 公共模板方法 ========================

    /**
     * 通用执行模板（有返回值），统一异常处理，自动释放连接
     */
    private <T> T execute(Function<Jedis, T> action) {
        try (Jedis jedis = jedisPool.getResource()) {
            return action.apply(jedis);
        } catch (Exception e) {
            log.error("Redis 操作失败：{}", e.getMessage(), e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 通用执行模板（无返回值），统一异常处理，自动释放连接
     */
    private void executeVoid(Consumer<Jedis> action) {
        try (Jedis jedis = jedisPool.getResource()) {
            action.accept(jedis);
        } catch (Exception e) {
            log.error("Redis 操作失败：{}", e.getMessage(), e);
            throw new RuntimeException("Redis 操作异常", e);
        }
    }

    /**
     * 批量新增 key，如果不存在则 set，并设置过期时间
     */
    public void setnxBatch(String prefix, Map<String, Object> objMap, int expireSeconds) {
        executeVoid(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            for (String key : objMap.keySet()) {
                String fullKey = prefix + key;
                pipeline.set(fullKey, JacksonUtil.toJSONString(objMap.get(key)), SetParams.setParams().nx().ex(expireSeconds));
            }
            pipeline.sync();
        });
    }

    /**
     * 批量设置多个 key 的过期时间（使用 Pipeline）
     *
     * @param keyPrefix     键前缀（如 "user_route:"）
     * @param keys          用户ID集合
     * @param expireSeconds 过期时间（秒）
     */
    public void expireBatch(String keyPrefix, List<String> keys, long expireSeconds) {
        executeVoid(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            for (String key : keys) {
                pipeline.expire(keyPrefix + key, (int) expireSeconds);
            }
            pipeline.sync();
        });
    }

    /**
     * 批量删除多个 key （使用 Pipeline）
     *
     * @param keyPrefix 键前缀（如 "user_route:"）
     * @param keys      用户ID集合
     */
    public void deleteBatch(String keyPrefix, List<String> keys) {
        executeVoid(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            for (String key : keys) {
                pipeline.del(keyPrefix + key);
            }
            pipeline.sync();
        });
    }

    // ======================== 字符串相关 ========================

    /**
     * 设置字符串值（无过期）
     */
    public void set(String key, String value) {
        executeVoid(jedis -> jedis.set(key, value));
    }


    /**
     * 设置字符串值（带过期时间）
     */
    public void setEx(String key, String value, long seconds) {
        executeVoid(jedis -> jedis.setex(key, (int) seconds, value));
    }

    /**
     * 获取字符串值
     */
    public String get(String key) {
        return execute(jedis -> jedis.get(key));
    }

    /**
     * 自增操作（用于计数器）
     */
    public long incr(String key) {
        return execute(jedis -> jedis.incr(key));
    }

    /**
     * 判断 key 是否存在
     */
    public boolean exists(String key) {
        return execute(jedis -> jedis.exists(key));
    }

    /**
     * 重命名 key（原子操作）
     */
    public String rename(String oldKey, String newKey) {
        return execute(jedis -> jedis.rename(oldKey, newKey));
    }

    /**
     * 设置过期时间（秒）
     */
    public void expire(String key, long seconds) {
        executeVoid(jedis -> jedis.expire(key, (int) seconds));
    }

    /**
     * 移除过期时间
     */
    public void persist(String key) {
        executeVoid(jedis -> jedis.persist(key));
    }

    /**
     * 如果 key 不存在则设置，并设置过期时间
     */
    public void setnxWithTimeOut(String key, String value, long timeoutSeconds) {
        executeVoid(jedis -> {
            if (jedis.setnx(key, value) != 0) {
                jedis.expire(key, (int) timeoutSeconds);
            }
        });
    }

    // ======================== 删除操作 ========================

    /**
     * 删除字符串类型 key
     */
    public long del(String key) {
        return execute(jedis -> jedis.del(key));
    }

    /**
     * 删除二进制 key（通常用于图片、文件等）
     */
    public void deleteByKey(int dbIndex, byte[] key) {
        executeVoid(jedis -> {
            jedis.select(dbIndex);
            jedis.del(key);
        });
    }

    // ======================== 二进制操作 ========================

    /**
     * 存储二进制值（可设置过期时间）
     */
    public void set(byte[] key, byte[] value, int dbIndex, long expireSeconds) {
        executeVoid(jedis -> {
            jedis.select(dbIndex);
            jedis.set(key, value);
            if (expireSeconds > 0) {
                jedis.expire(key, (int) expireSeconds);
            }
        });
    }

    /**
     * 获取二进制值
     */
    public byte[] get(int dbIndex, byte[] key) {
        return execute(jedis -> {
            jedis.select(dbIndex);
            return jedis.get(key);
        });
    }

    // ======================== List 列表操作 ========================

    /**
     * 向列表尾部插入值
     */
    public long rpush(String key, String value) {
        return execute(jedis -> jedis.rpush(key, value));
    }

    // ======================== Hash 哈希操作 ========================

    /**
     * 设置 HashMap 数据
     */
    public String hmset(String key, Map<String, String> map) {
        return execute(jedis -> jedis.hmset(key, map));
    }

    // ======================== SortedSet 有序集合 ========================

    /**
     * 向有序集合添加成员
     */
    public void zadd(String key, String value, double score) {
        executeVoid(jedis -> jedis.zadd(key, score, value));
    }

    /**
     * 删除指定成员
     */
    public long zrem(String key, String... values) {
        return execute(jedis -> jedis.zrem(key, values));
    }

    /**
     * 获取集合元素数量
     */
    public long zcard(String key) {
        return execute(jedis -> jedis.zcard(key));
    }

    // ======================== HyperLogLog ========================

    /**
     * 添加元素到 HyperLogLog
     */
    public void pfadd(String key, String element) {
        executeVoid(jedis -> jedis.pfadd(key, element));
    }

    /**
     * 获取 HyperLogLog 估算数量
     */
    public long pfcount(String key) {
        return execute(jedis -> jedis.pfcount(key));
    }

    // ======================== 时间操作 ========================

    /**
     * 获取 Redis 服务器当前时间（秒）
     */
    public long currentTimeSecond() {
        return execute(jedis -> {
            Object result = jedis.eval("return redis.call('TIME')", 0);
            if (result instanceof List<?> list && list.size() >= 1) {
                return Long.parseLong(String.valueOf(list.get(0)));
            }
            return 0L;
        });
    }
}