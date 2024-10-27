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
public class JedisUtil {
    private static final JedisUtil jedisUtil = new JedisUtil();
    private JedisPool jedisPool = null;

    //写成静态代码块形式，只加载一次，节省资源
//    static {
////        Properties properties = PropertyUtil.loadProperties("redis.properties");
//        String host = "localhost";
//        String port = "6379";
//        String pass = "19940806";
//        String timeout = properties.getProperty("redis.timeout");
//        String maxIdle = properties.getProperty("redis.maxIdle");
//        String maxTotal = properties.getProperty("redis.maxTotal");
//        String maxWaitMillis = properties.getProperty("redis.maxWaitMillis");
//        String testOnBorrow = properties.getProperty("redis.testOnBorrow");
//
//        JedisPoolConfig config = new JedisPoolConfig();
    //控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
    //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
//        config.setMaxTotal(Integer.parseInt(maxTotal));
//        //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
//        config.setMaxIdle(Integer.parseInt(maxIdle));
//        //表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
//        config.setMaxWaitMillis(Long.parseLong(maxWaitMillis));
//        //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
//        config.setTestOnBorrow(Boolean.valueOf(testOnBorrow));
//
//        this.jedisPool = new JedisPool(config, host, Integer.parseInt(port), 10000);
//    }

    private JedisUtil() {
        IMRedisConfig.RedisConfig redis = ConfigCenter.redisConfig.getRedis();
        JedisPoolConfig config = new JedisPoolConfig();

        // 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
        config.setMaxTotal(100);
        // 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        config.setMaxIdle(10);
        // 表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
        config.setMaxWaitMillis(10000L);
        // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
        config.setTestOnBorrow(true);

        this.jedisPool = new JedisPool(config, redis.getHost(), redis.getPort(), 10000);
    }

    /**
     * 获取JedisUtil实例
     *
     * @return
     */
    public static JedisUtil getInstance() {
        return jedisUtil;
    }

    /**
     * 从jedis连接池中获取获取jedis对象
     *
     * @return
     */
    private Jedis getJedis() {
        return jedisPool.getResource();
    }

    /**
     * 回收jedis(放到finally中)
     *
     * @param jedis
     */
    private void returnJedis(Jedis jedis) {
        if (null != jedis && null != jedisPool) {
            // jedisPool.returnResource(jedis);
            jedis.close();
        }
    }

    /**
     * 销毁连接(放到catch中)
     *
     * @param jedis
     */
    private void returnBrokenResource(Jedis jedis) {
        if (null != jedis && null != jedisPool) {
            jedisPool.returnResource(jedis);
        }
    }

    /**
     * 添加sorted set
     *
     * @param key
     * @param value
     * @param score
     */
    public void zadd(String key, String value, double score) {
        Jedis jedis = getJedis();
        jedis.zadd(key, score, value);
        returnJedis(jedis);
    }

    /**
     * 返回指定位置的集合元素,0为第一个元素，-1为最后一个元素
     * @param key
     * @param start
     * @param end
     * @return
     */
//    public Set<String> zrange(String key, int start, int end) {
//        Jedis jedis = getJedis();
//        Set<String> set = jedis.zrange(key, start, end);
//        returnJedis(jedis);
//        return set;
//    }

    /**
     * 获取给定区间的元素，原始按照权重由高到低排序
     * @param key
     * @param start
     * @param end
     * @return
     */
//    public Set<String> zrevrange(String key, int start, int end) {
//        Jedis jedis = getJedis();
//        Set<String> set = jedis.zrevrange(key, start, end);
//        returnJedis(jedis);
//        return set;
//    }

    /**
     * 添加对应关系，如果对应关系已存在，则覆盖
     *
     * @param key
     * @param map 对应关系
     * @return 状态，成功返回OK
     */
    public String hmset(String key, Map<String, String> map) {
        Jedis jedis = getJedis();
        String s = jedis.hmset(key, map);
        returnJedis(jedis);
        return s;
    }

    /**
     * 向List头部追加记录
     *
     * @param key
     * @param value
     * @return 记录总数
     */
    public long rpush(String key, String value) {
        Jedis jedis = getJedis();
        long count = jedis.rpush(key, value);
        returnJedis(jedis);
        return count;
    }

    /**
     * 向List头部追加记录
     *
     * @param key
     * @param value
     * @return 记录总数
     */
    private long rpush(byte[] key, byte[] value) {
        Jedis jedis = getJedis();
        long count = jedis.rpush(key, value);
        returnJedis(jedis);
        return count;
    }

    /**
     * 删除
     *
     * @param key
     * @return
     */
    public long del(String key) {
        Jedis jedis = getJedis();
        long s = jedis.del(key);
        returnJedis(jedis);
        return s;
    }

    /**
     * 从集合中删除成员
     *
     * @param key
     * @param value
     * @return 返回1成功
     */
    public long zrem(String key, String... value) {
        Jedis jedis = getJedis();
        long s = jedis.zrem(key, value);
        returnJedis(jedis);
        return s;
    }

    public void saveValueByKey(int dbIndex, byte[] key, byte[] value, int expireTime)
            throws Exception {
        Jedis jedis = null;
        boolean isBroken = false;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.set(key, value);
            if (expireTime > 0)
                jedis.expire(key, expireTime);
        } catch (Exception e) {
            isBroken = true;
            throw e;
        } finally {
            returnResource(jedis, isBroken);
        }
    }

    public byte[] getValueByKey(int dbIndex, byte[] key) throws Exception {
        Jedis jedis = null;
        byte[] result = null;
        boolean isBroken = false;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            result = jedis.get(key);
        } catch (Exception e) {
            isBroken = true;
            throw e;
        } finally {
            returnResource(jedis, isBroken);
        }
        return result;
    }

    public void deleteByKey(int dbIndex, byte[] key) throws Exception {
        Jedis jedis = null;
        boolean isBroken = false;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.del(key);
        } catch (Exception e) {
            isBroken = true;
            throw e;
        } finally {
            returnResource(jedis, isBroken);
        }
    }

    public void returnResource(Jedis jedis, boolean isBroken) {
        if (jedis == null)
            return;
        if (isBroken)
            jedisPool.returnBrokenResource(jedis);
        else
            jedisPool.returnResource(jedis);
    }

    /**
     * 获取总数量
     *
     * @param key
     * @return
     */
    public long zcard(String key) {
        Jedis jedis = getJedis();
        long count = jedis.zcard(key);
        returnJedis(jedis);
        return count;
    }

    /**
     * 是否存在KEY
     *
     * @param key
     * @return
     */
    public boolean exists(String key) {
        Jedis jedis = getJedis();
        boolean exists = jedis.exists(key);
        returnJedis(jedis);
        return exists;
    }

    /**
     * 重命名KEY
     *
     * @param oldKey
     * @param newKey
     * @return
     */
    public String rename(String oldKey, String newKey) {
        Jedis jedis = getJedis();
        String result = jedis.rename(oldKey, newKey);
        returnJedis(jedis);
        return result;
    }

    /**
     * 设置失效时间
     *
     * @param key
     * @param seconds
     */
    public void expire(String key, int seconds) {
        Jedis jedis = getJedis();
        jedis.expire(key, seconds);
        returnJedis(jedis);
    }

    /**
     * 删除失效时间
     *
     * @param key
     */
    public void persist(String key) {
        Jedis jedis = getJedis();
        jedis.persist(key);
        returnJedis(jedis);
    }

    /**
     * 添加一个键值对，如果键存在不在添加，如果不存在，添加完成以后设置键的有效期
     *
     * @param key
     * @param value
     * @param timeOut
     */
    public void setnxWithTimeOut(String key, String value, int timeOut) {
        Jedis jedis = getJedis();
        if (0 != jedis.setnx(key, value)) {
            jedis.expire(key, timeOut);
        }
        returnJedis(jedis);
    }

    /**
     * 返回指定key序列值
     *
     * @param key
     * @return
     */
    public long incr(String key) {
        Jedis jedis = getJedis();
        long l = jedis.incr(key);
        returnJedis(jedis);
        return l;
    }

    public void set(String key, String value) {
        Jedis jedis = getJedis();
        jedis.set(key, value);
        returnJedis(jedis);
    }

    public void setEx(String key, String value) {
        Jedis jedis = getJedis();
        jedis.setex(key, 60 * 60 * 24, value);
        returnJedis(jedis);
    }

    public void get(String key) {
        Jedis jedis = getJedis();
        jedis.get(key);
        returnJedis(jedis);
    }


    /**
     * 获取当前时间
     *
     * @return 秒
     */
    public long currentTimeSecond() {
        Long l = 0l;
        Jedis jedis = getJedis();
        Object obj = jedis.eval("return redis.call('TIME')", 0);
        if (obj != null) {
            List<String> list = (List) obj;
            l = Long.valueOf(list.get(0));
        }
        returnJedis(jedis);
        return l;
    }
}