package com.xy.generator.core.impl;

import com.xy.generator.core.IDGen;
import com.xy.generator.model.IdMetaInfo;
import com.xy.generator.model.Segment;
import com.xy.generator.repository.IdMetaInfoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Redis 与数据库双 Buffer 机制的全局唯一 ID 生成器，
 * 使用 Redisson 分布式锁确保多实例部署时仅由一个实例执行校准操作。
 */
@Slf4j
@Component("redisSegmentIDGen")
public class RedisSegmentIDGenImpl implements IDGen<Long> {

    /**
     * 分布式锁前缀
     */
    private final String LOCK_PREFIX = "lock:idgen:calibrate:";
    /**
     * 本地缓存每个业务 key 的 SegmentPair
     */
    private final ConcurrentHashMap<String, SegmentPair> segmentCache = new ConcurrentHashMap<>();
    /**
     * Reactor 调度池，用于运行阻塞操作
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "IDGen-Scheduler");
        t.setDaemon(true);
        return t;
    });
    @Resource
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    @Resource
    private IdMetaInfoRepository idMetaInfoRepository;
    @Resource
    private RedissonClient redissonClient;
    /**
     * 默认每个号段大小，支持配置注入
     */
    @Value("${generate.step:1000}")
    private Integer defaultStep;
    /**
     * 初始id
     */
    @Value("${generate.initialId:0}")
    private Long initialId;
    /**
     * 预加载阈值（剩余比例）
     */
    @Value("${generate.prefetchThreshold:0.2}")
    private double prefetchThreshold;
    /**
     * 分布式锁等待时间（秒）
     */
    @Value("${generate.lockWaitSeconds:5}")
    private long lockWaitSeconds;
    /**
     * 分布式锁租约时间（秒）
     */
    @Value("${generate.lockLeaseSeconds:60}")
    private long lockLeaseSeconds;

    /**
     * 初始化时检查 Redis 可用性
     */
    @SneakyThrows
    @PostConstruct
    @Override
    public boolean init() {
        String pong = reactiveRedisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .ping().block();
        if (pong == null) {
            throw new IllegalStateException("Redis 服务连接失败，PING 返回 null");
        }

        log.info("Redis 服务可用，PING 响应：{}", pong);
        return true;
    }

    /**
     * 获取下一个 ID（响应式返回）
     */
    @Override
    public Mono<Long> get(String key) {
        // 取本地缓存 segmentPair，没有则初始化
        SegmentPair pair = segmentCache.computeIfAbsent(key, SegmentPair::new);
        Long nextId = pair.nextId();
        return Mono.just(nextId);
    }

    /**
     * 本地缓存的号段容器，双缓冲结构
     */
    private class SegmentPair {

        private final String key;
        private final AtomicBoolean loading = new AtomicBoolean(false);
        /**
         * 当前号池
         */
        private volatile Segment current;
        /**
         * 缓冲号池
         */
        private volatile Segment next;

        public SegmentPair(String key) {
            this.key = key;
            // 首次加载当前号段（阻塞）
            this.current = loadSegmentBlocking();
        }

        /**
         * 获取下一个可用 ID
         */
        public Long nextId() {

            int retry = 0;

            while (true) {
                Segment seg = current;
                Long nextId = seg.next();
                if (nextId != -1) {
                    // 如果剩余不足 20%，提前异步加载下一个 Segment
                    if (seg.remaining() < seg.getStep() * prefetchThreshold) {
                        log.info("[{}] 余量不足{}%，获取id：{}", key, prefetchThreshold * 100, nextId);
                        triggerAsyncLoad();
                    } else {
                        log.info("[{}] 获取id：{}", key, nextId);
                    }
                    return nextId;
                }

                // 当前段耗尽，切换到下一段
                synchronized (this) {
                    if (current == seg && next != null) {
                        log.info("[{}] 切换号段：{}-{} -> {}-{}", key, seg.getStart(), seg.getEnd(), next.getStart(), next.getEnd());
                        current = next;
                        next = null;
                        continue;
                    }
                }

                // 等待号段加载完成
                if (retry++ > 50) {
                    throw new IllegalStateException("号段耗尽，且新的号段尚未准备好，请稍后重试！");
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待号段加载被中断", e);
                }
            }
        }

        /**
         * 异步加载新的号段
         */
        private void triggerAsyncLoad() {
            if (next == null && loading.compareAndSet(false, true)) {
                Mono.fromCallable(this::loadSegmentBlocking)
                        .doOnSuccess(seg -> {
                            synchronized (this) {
                                next = seg;
                            }
                            log.info("[{}] 异步预加载完成：{}-{}", key, seg.getStart(), seg.getEnd());
                        })
                        .doFinally(s -> loading.set(false))
                        .subscribe();
            }
        }

        /**
         * 在异步线程中加载号段（Redis 和 DB 读写）
         */
        private Segment loadSegmentBlocking() {
            String lockName = LOCK_PREFIX + key;

            RLock lock = redissonClient.getLock(lockName);

            try {
                // 获取分布式锁，带等待与租约
                if (!lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("获取分布式锁超时：" + lockName);
                }

                log.debug("[{}] 获得分布式锁，开始校准与分配", key);

                // === 1. 查询数据库中的元信息（阻塞，线程池中执行） ===
                IdMetaInfo meta = idMetaInfoRepository.findById(key).orElseGet(() -> {
                    IdMetaInfo m = new IdMetaInfo();
                    m.setId(key);
                    m.setMaxId(initialId);
                    m.setStep(defaultStep);
                    m.setUpdateTime(LocalDateTime.now());
                    log.info("[{}] 元信息不存在，已初始化 step={}", key, defaultStep);
                    return m;
                });

                // === 2. Redis 中获取当前值（响应式操作，用 .get().toFuture().get() 转换为阻塞） ===
                Object redisObj = reactiveRedisTemplate.opsForValue().get(key)
                        .toFuture()
                        .get(); // ⚠️ 安全阻塞，因本方法本就在线程池中

                // redis 序列化会将long类型 序列成 int型
                Long redisVal = (redisObj instanceof Integer) ? ((Integer) redisObj).longValue() : (Long) redisObj;

                // === 3. 如果 Redis 中的值落后于 DB，强制设置 Redis 值 ===
                if (redisVal < meta.getMaxId()) {
                    long reset = meta.getMaxId();
                    reactiveRedisTemplate.opsForValue().set(key, reset).subscribe();
                    log.warn("[{}] Redis 值校准：{} -> {}", key, redisVal, reset);
                }

                // === 4. Redis 自增以获得新号段的最大值 ===
                Long newMax = reactiveRedisTemplate.opsForValue().increment(key, meta.getStep())
                        .toFuture()
                        .get();
                long start = newMax - meta.getStep() + 1;
                long end = newMax;

                // 5. 异步持久化更新数据库
                scheduler.submit(() -> {
                    try {
                        meta.setMaxId(end);
                        meta.setUpdateTime(LocalDateTime.now());
                        idMetaInfoRepository.save(meta);
                        log.info("[{}] 元信息持久化成功 开始:{} 结束:{} 步长:{}", key, start, end, meta.getStep());
                    } catch (Exception ex) {
                        log.error("[{}] 元信息保存失败: {}", key, ex.getMessage(), ex);
                    }
                });

                return new Segment(start, end, meta.getStep());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("加载 Segment 异常", e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("[{}] 释放分布式锁", key);
                }
            }
        }
    }
}