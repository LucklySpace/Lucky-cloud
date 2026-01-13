package com.xy.lucky.crypto.core.sign.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Nonce 防重放缓存
 * <p>
 * 简单的内存实现，生产环境建议使用 Redis 等分布式缓存
 */
public class NonceCache {

    /**
     * nonce 缓存 Map
     */
    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    /**
     * 过期时间（秒）
     */
    private final long expireSeconds;

    /**
     * 清理任务调度器
     */
    private final ScheduledExecutorService scheduler;

    public NonceCache(long expireSeconds) {
        this.expireSeconds = expireSeconds;

        // 启动定期清理任务
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nonce-cache-cleaner");
            t.setDaemon(true);
            return t;
        });

        // 每分钟清理一次过期的 nonce
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 检查 nonce 是否存在（如果不存在则添加）
     *
     * @param nonce nonce 值
     * @return true 表示 nonce 已存在（重复请求），false 表示 nonce 不存在（首次请求）
     */
    public boolean checkAndSet(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return true; // 空 nonce 视为重复
        }

        long now = System.currentTimeMillis();
        Long existing = cache.putIfAbsent(nonce, now);
        return existing != null; // 如果已存在返回 true
    }

    /**
     * 检查 nonce 是否存在
     */
    public boolean exists(String nonce) {
        return cache.containsKey(nonce);
    }

    /**
     * 添加 nonce
     */
    public void add(String nonce) {
        cache.put(nonce, System.currentTimeMillis());
    }

    /**
     * 清理过期的 nonce
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        long expireMillis = expireSeconds * 1000;

        cache.entrySet().removeIf(entry ->
                (now - entry.getValue()) > expireMillis
        );
    }

    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 关闭清理任务
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}

