package com.xy.connect.utils;

import com.xy.connect.config.ConfigCenter;
import com.xy.connect.config.IMRedisConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Map;

/**
 * redis工具类
 *
 * @author lc
 */
public final class JedisUtil {
    private static final JedisUtil INSTANCE = new JedisUtil();
    private final JedisPool jedisPool;

    private JedisUtil() {
        // 从配置中心获取 Redis 配置
        IMRedisConfig.RedisConfig redis = ConfigCenter.redisConfig.getRedis();
        JedisPoolConfig config = new JedisPoolConfig();
        // 设置连接池参数
        config.setMaxTotal(100);         // 最大连接数
        config.setMaxIdle(10);           // 最大空闲连接数
        config.setMaxWaitMillis(10000L); // 最大等待时间
        config.setTestOnBorrow(true);    // 借出前验证连接有效性

        // 初始化 JedisPool
        this.jedisPool = new JedisPool(config, redis.getHost(), redis.getPort(), 10000, redis.getPassword());
    }

    /**
     * 获取 JedisUtil 实例（单例）
     */
    public static JedisUtil getInstance() {
        return INSTANCE;
    }

    /**
     * 添加 sorted set 成员
     */
    public void zadd(String key, String value, double score) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd(key, score, value);
        }
    }

    /**
     * 设置 hash 对应关系（覆盖已有）
     */
    public String hmset(String key, Map<String, String> map) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hmset(key, map);
        }
    }

    /**
     * 向列表尾部追加记录
     */
    public long rpush(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.rpush(key, value);
        }
    }

    /**
     * 删除指定 key
     */
    public long del(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(key);
        }
    }

    /**
     * 从 sorted set 中删除指定成员
     */
    public long zrem(String key, String... value) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrem(key, value);
        }
    }

    /**
     * 保存二进制值，同时设置有效期（单位：秒）
     */
    public void saveValueByKey(int dbIndex, byte[] key, byte[] value, long expireTime) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(dbIndex);
            jedis.set(key, value);
            if (expireTime > 0) {
                jedis.expire(key, expireTime);
            }
        }
    }

    /**
     * 获取二进制值
     */
    public byte[] getValueByKey(int dbIndex, byte[] key) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(dbIndex);
            return jedis.get(key);
        }
    }

    /**
     * 根据 key 删除数据（针对二进制 key）
     */
    public void deleteByKey(int dbIndex, byte[] key) throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(dbIndex);
            jedis.del(key);
        }
    }

    /**
     * 获取 sorted set 元素总数
     */
    public long zcard(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zcard(key);
        }
    }

    /**
     * 判断 key 是否存在
     */
    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        }
    }

    /**
     * 重命名 key
     */
    public String rename(String oldKey, String newKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.rename(oldKey, newKey);
        }
    }

    /**
     * 为 key 设置失效时间（单位：秒）
     */
    public void expire(String key, long seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(key, seconds);
        }
    }

    /**
     * 删除 key 的失效时间
     */
    public void persist(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.persist(key);
        }
    }

    /**
     * 如果 key 不存在则设置，并设置超时时间
     */
    public void setnxWithTimeOut(String key, String value, long timeOut) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (jedis.setnx(key, value) != 0) {
                jedis.expire(key, timeOut);
            }
        }
    }

    /**
     * 对 key 执行自增操作
     */
    public long incr(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr(key);
        }
    }

    /**
     * 设置 key 的值
     */
    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }
    }

    /**
     * 设置 key 的值，并设定过期时间（秒）
     */
    public void setEx(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, 60 * 60 * 24L, value);
        }
    }

    /**
     * 获取 key 的值
     */
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    /**
     * 获取当前时间（秒），通过执行 Redis 的 TIME 命令
     */
    public long currentTimeSecond() {
        try (Jedis jedis = jedisPool.getResource()) {
            Object obj = jedis.eval("return redis.call('TIME')", 0);
            if (obj != null) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) obj;
                return Long.parseLong(list.get(0));
            }
            return 0L;
        }
    }

    /**
     * 向 HyperLogLog 中添加成员（用于统计）
     */
    public void pfadd(String hyperloglogKey, String userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.pfadd(hyperloglogKey, userId);
        }
    }

    /**
     * 获取 HyperLogLog 的统计值
     */
    public long pfcount(String hyperloglogKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.pfcount(hyperloglogKey);
        }
    }
}
