package com.xy.lucky.leaf.core.impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.core.model.IMetaId;
import com.xy.lucky.leaf.core.IDGen;
import com.xy.lucky.leaf.model.IdMetaInfo;
import com.xy.lucky.leaf.repository.IdMetaInfoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 * 高性能 Redis Segment ID 生成器
 *
 * 特性：
 * - 双缓冲区设计，提高并发性能
 * - 异步预加载，减少等待时间
 * - 本地缓存和持久化，提高系统可靠性
 * - 基于Redis的分布式锁，确保多节点安全
 *
 * 核心设计：
 * - LocalSegment 使用 AtomicLong，无锁 next()
 * - 使用共享 loaderPool 来异步加载号段
 * - 持久化到文件使用定时批量 flush，避免每次 get 请求都写文件
 */
@Slf4j
@Component("redisSegmentIDGen")
public class RedisSegmentIDGenImpl implements IDGen {

    // 本地持久化文件（用于快速恢复）
    private static final String CACHE_FILE = "idgen-segments.json";
    private static final long DEFAULT_PERSIST_INTERVAL_SECONDS = 5L;

    private static final String LOCK_PREFIX = "lock:idgen:calibrate:";

    // 本地缓存的段
    private final ConcurrentHashMap<String, SegmentPair> segmentCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 线程池
    // loaderPool: 固定大小，避免过多并发 DB/Redis 操作；默认 CPU*2
    private final ExecutorService loaderPool;

    // 定时任务调度器（用于持久化）
    private final ScheduledExecutorService scheduler;

    // 持久化脏数据标记
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    @Resource
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Resource
    private IdMetaInfoRepository idMetaInfoRepository;

    @Resource
    private RedissonClient redissonClient;

    @Value("${generate.step:1000}")
    private int defaultStep;

    @Value("${generate.initialId:0}")
    private long initialId;

    @Value("${generate.prefetchThreshold:0.2}")
    private double prefetchThreshold;

    @Value("${generate.lockWaitSeconds:5}")
    private long lockWaitSeconds;

    @Value("${generate.lockLeaseSeconds:60}")
    private long lockLeaseSeconds;

    /**
     * 构造函数
     * 初始化线程池
     */
    public RedisSegmentIDGenImpl() {
        int cpu = Math.max(1, Runtime.getRuntime().availableProcessors());
        // loaderPool: 允许一定并行度但避免冲击 redis/db
        this.loaderPool = Executors.newFixedThreadPool(Math.min(16, cpu * 2),
                r -> {
                    Thread t = new Thread(r, "IDGen-Loader");
                    t.setDaemon(true);
                    return t;
                });
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "IDGen-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    @SneakyThrows
    @PostConstruct
    public boolean init() {
        // 从文件快速加载状态（尽力而为）
        loadCacheFromFile();

        // 安排定期持久化任务（防抖动）
        scheduler.scheduleAtFixedRate(this::persistCacheIfDirty, DEFAULT_PERSIST_INTERVAL_SECONDS,
                DEFAULT_PERSIST_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 在后台测试redis连接（不阻塞启动）
        loaderPool.submit(() -> {
            try {
                String pong = reactiveRedisTemplate.getConnectionFactory()
                        .getReactiveConnection()
                        .ping().block(Duration.ofSeconds(2));
                log.info("Redis PING -> {}", pong);
            } catch (Throwable t) {
                log.warn("Redis ping failed during init: {}", t.getMessage());
            }
        });

        log.info("RedisSegmentIDGen initialized (loaderPool={}, persistInterval={}s)",
                ((ThreadPoolExecutor) loaderPool).getCorePoolSize(), DEFAULT_PERSIST_INTERVAL_SECONDS);
        return true;
    }

    /**
     * 从文件加载缓存
     */
    private void loadCacheFromFile() {
        try {
            File f = Paths.get(CACHE_FILE).toFile();
            if (!f.exists()) return;
            Map<String, SegmentSnapshot> map = objectMapper.readValue(f,
                    new TypeReference<Map<String, SegmentSnapshot>>() {
                    });
            if (map != null && !map.isEmpty()) {
                map.forEach((k, v) -> {
                    SegmentPair pair = new SegmentPair(k, v);
                    segmentCache.put(k, pair);
                });
                log.info("Loaded {} segment snapshots from {}", map.size(), CACHE_FILE);
            }
        } catch (Throwable t) {
            log.warn("Failed to load segment cache from file: {}", t.getMessage());
        }
    }

    /**
     * 定期持久化（如果数据有变化）
     */
    private void persistCacheIfDirty() {
        if (!dirty.getAndSet(false)) return;
        persistCacheToFile();
    }

    /**
     * 持久化缓存到文件
     */
    private synchronized void persistCacheToFile() {
        try {
            Map<String, SegmentSnapshot> snap = segmentCache.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().snapshot()));
            objectMapper.writeValue(new File(CACHE_FILE), snap);
            log.debug("Persisted {} segments to {}", snap.size(), CACHE_FILE);
        } catch (Throwable t) {
            log.error("Persist cache to file failed: {}", t.getMessage(), t);
            dirty.set(true); // 标记为脏数据，用于重试
        }
    }

    @Override
    public Mono<IMetaId> get(String key) {
        return Mono.fromCallable(() -> getId(key));
    }

    @Override
    public IMetaId getId(String key) {
        // 快速路径获取或创建段对
        SegmentPair pair = segmentCache.computeIfAbsent(key, SegmentPair::new);
        long id = pair.nextId();
        // 防抖动持久化：标记为脏数据，实际写入定期执行
        dirty.set(true);
        return IMetaId.builder().longId(id).build();
    }

    /**
     * 关闭服务时清理资源
     */
    public void shutdown() {
        try {
            loaderPool.shutdownNow();
        } catch (Throwable ignored) {
        }
        try {
            scheduler.shutdownNow();
        } catch (Throwable ignored) {
        }
    }

    // ----------------------------
    // 用于持久化的快照结构
    // ----------------------------
    @Data
    public static class SegmentSnapshot {
        private long currentStart;
        private long currentEnd;
        private long currentCursor; // 下一个要使用的值
        private int currentStep;

        private Long nextStart;
        private Long nextEnd;
        private Integer nextStep;
    }

    /**
     * 本地段：内存轻量、线程安全、无锁的号段实现
     */
    private static final class LocalSegment {
        static final long EXHAUSTED = Long.MIN_VALUE;

        final long start;
        final long end;
        final int step;
        final AtomicLong cursor; // 下一个要返回的ID

        LocalSegment(long start, long end, int step) {
            this.start = start;
            this.end = end;
            this.step = step;
            this.cursor = new AtomicLong(Math.max(start, start)); // 下一个要返回的
        }

        // 使用已知当前游标构造
        LocalSegment(long start, long end, int step, long currentCursor) {
            this.start = start;
            this.end = end;
            this.step = step;
            this.cursor = new AtomicLong(Math.max(currentCursor, start));
        }

        /**
         * 获取下一个ID
         *
         * @return 下一个ID，如果段已耗尽则返回EXHAUSTED
         */
        long next() {
            while (true) {
                long cur = cursor.get();
                if (cur > end) return EXHAUSTED;
                if (cursor.compareAndSet(cur, cur + 1)) {
                    return cur;
                }
                // CAS 失败，重试（极少数情况）
            }
        }

        /**
         * 获取剩余ID数量
         * @return 剩余ID数量
         */
        long remaining() {
            long cur = cursor.get();
            long rem = end - cur + 1;
            return Math.max(0, rem);
        }

        /**
         * 获取步长
         * @return 步长
         */
        int getStep() {
            return this.step;
        }
    }

    // ----------------------------
    // 段对和本地段
    // ----------------------------
    private class SegmentPair {
        private final String key;

        // 加载标记
        private final AtomicBoolean loading = new AtomicBoolean(false);

        // 使用volatile保证可见性；LocalSegment是线程安全的
        private volatile LocalSegment current;
        private volatile LocalSegment nextSegment;

        SegmentPair(String key) {
            this.key = key;
            // 同步加载初始段，但在loaderPool中执行以避免阻塞调用线程（如果DB/Redis较慢）
            Future<LocalSegment> f = loaderPool.submit(this::loadSegmentBlocking);
            try {
                this.current = f.get(3, TimeUnit.SECONDS); // 初始加载的小超时
            } catch (Throwable t) {
                log.warn("[{}] initial load slow or failed, creating fallback empty segment", key);
                // 回退到一个空段，将在首次访问时触发异步加载
                this.current = new LocalSegment(initialId + 1, initialId, defaultStep);
                triggerAsyncLoad(); // 主动加载
            }
        }

        SegmentPair(String key, SegmentSnapshot snap) {
            this.key = key;
            this.current = new LocalSegment(snap.getCurrentStart(), snap.getCurrentEnd(),
                    snap.getCurrentStep(), snap.getCurrentCursor());
            if (snap.getNextStart() != null) {
                this.nextSegment = new LocalSegment(snap.getNextStart(), snap.getNextEnd(),
                        snap.getNextStep());
            }
        }

        /**
         * 获取下一个ID
         * @return 下一个ID
         */
        long nextId() {
            int retry = 0;

            while (true) {
                // 快速路径：读取 current（可能被其他线程更新）
                LocalSegment seg = this.current;
                long id = seg.next(); // 调用 LocalSegment.next() 方法

                if (id != LocalSegment.EXHAUSTED) {
                    // 当剩余量低于阈值时触发异步加载（非阻塞）
                    if (seg.remaining() < seg.getStep() * prefetchThreshold) {
                        triggerAsyncLoad();
                    }
                    return id;
                }

                // 当前段耗尽，尝试快速切换到已加载的 nextSegment（如果存在）
                LocalSegment ns = this.nextSegment;
                if (ns != null) {
                    // 在临界区内再次确认并执行切换（双重检查）
                    synchronized (this) {
                        if (this.current == seg && this.nextSegment != null) {
                            this.current = this.nextSegment;
                            this.nextSegment = null;
                            // 切换成功，立即重试以从新的 current 取值
                            continue;
                        }
                    }
                    // 如果在同步区发现已经被切换走，loop 将重试并读取新的 current
                    continue;
                }

                // 没有可切换的 nextSegment，则触发异步加载（若尚未进行）
                triggerAsyncLoad();

                // 轻量等待：短自旋 + 退避（避免忙等）
                retry++;
                if (retry > 200) { // 可配置的上限
                    throw new IllegalStateException("Segment exhausted and new segment not ready for key=" + key);
                }
                // parkNanos 做短暂停顿，避免调用线程完全忙等
                // 逐步增加等待时间以降低 CPU 占用（指数退避或线性退避都可）
                long backoffNanos = Math.min(1_000L * retry, 1_000_000L); // 最多 1ms
                LockSupport.parkNanos(backoffNanos);
                // 重试循环会再次检查 current/nextSegment
            }
        }

        /**
         * 触发异步加载
         */
        private void triggerAsyncLoad() {
            // 仅当 nextSegment 为空并且没有正在加载时提交加载任务
            if (this.nextSegment == null && loading.compareAndSet(false, true)) {
                loaderPool.submit(() -> {
                    try {
                        LocalSegment seg = loadSegmentBlocking();
                        // 将加载到的新段放入 nextSegment（在 synchronized 中双重检查）
                        synchronized (this) {
                            if (this.nextSegment == null) {
                                this.nextSegment = seg;
                            } else {
                                // 如果已有 nextSegment（极小概率），丢弃新加载段或可合并策略
                                log.debug("[{}] nextSegment already present, discarding loaded segment {}-{}",
                                        key, seg.start, seg.end);
                            }
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("[{}] async prefetch done: {}-{}", key, seg.start, seg.end);
                        }
                    } catch (Throwable t) {
                        log.error("[{}] async load failed", key, t);
                    } finally {
                        loading.set(false);
                    }
                });
            }
        }

        /**
         * 阻塞式段加载 - 在loaderPool线程中运行
         * @return 加载的本地段
         */
        private LocalSegment loadSegmentBlocking() {
            String lockName = LOCK_PREFIX + key;
            RLock lock = redissonClient.getLock(lockName);
            boolean locked = false;
            try {
                locked = lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS);
                if (!locked) {
                    throw new IllegalStateException("Failed to acquire distributed lock: " + lockName);
                }

                // 1. 从数据库加载元数据（同步）
                IdMetaInfo meta = idMetaInfoRepository.findById(key).orElseGet(() -> {
                    IdMetaInfo m = new IdMetaInfo();
                    m.setId(key);
                    m.setMaxId(initialId);
                    m.setStep(defaultStep);
                    m.setUpdateTime(LocalDateTime.now());
                    idMetaInfoRepository.save(m);
                    log.info("[{}] meta not found, initialized step={}", key, defaultStep);
                    return m;
                });

                int step = Math.max(1, meta.getStep() == null ? defaultStep : meta.getStep());

                // 2. 检查redis当前值（在此阻塞但在线程池中运行）
                Object redisValObj = reactiveRedisTemplate.opsForValue().get(key).block(Duration.ofSeconds(2));
                if (redisValObj == null) {
                    // 使用meta.maxId初始化redis
                    reactiveRedisTemplate.opsForValue().set(key, meta.getMaxId()).block(Duration.ofSeconds(2));
                }

                // 3. 增加redis以分配新范围
                Long newMax = reactiveRedisTemplate.opsForValue().increment(key, step).block(Duration.ofSeconds(3));
                if (newMax == null) {
                    throw new IllegalStateException("Redis increment returned null for key=" + key);
                }
                long start = newMax - step + 1;
                long end = newMax;

                // 4. 异步持久化meta.maxId（不阻塞调用者）
                try {
                    scheduler.submit(() -> {
                        try {
                            meta.setMaxId(end);
                            meta.setUpdateTime(LocalDateTime.now());
                            idMetaInfoRepository.save(meta);
                        } catch (Throwable ex) {
                            log.error("[{}] persist meta failed: {}", key, ex.getMessage(), ex);
                        }
                    });
                } catch (RejectedExecutionException rx) {
                    log.warn("[{}] persist meta scheduling rejected, will persist later", key);
                }

                // 返回新的本地段
                return new LocalSegment(start, end, step);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while acquiring lock", ie);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    try {
                        lock.unlock();
                    } catch (Throwable ignore) {
                    }
                }
            }
        }

        /**
         * 创建快照
         * @return 段快照
         */
        SegmentSnapshot snapshot() {
            SegmentSnapshot snap = new SegmentSnapshot();
            LocalSegment cur = current;
            snap.setCurrentStart(cur.start);
            snap.setCurrentEnd(cur.end);
            snap.setCurrentCursor(cur.cursor.get());
            snap.setCurrentStep(cur.step);
            LocalSegment n = nextSegment;
            if (n != null) {
                snap.setNextStart(n.start);
                snap.setNextEnd(n.end);
                snap.setNextStep(n.step);
            }
            return snap;
        }
    }
}
