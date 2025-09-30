package com.xy.generator.core.impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.core.model.IMetaId;
import com.xy.generator.core.IDGen;
import com.xy.generator.model.IdMetaInfo;
import com.xy.generator.repository.IdMetaInfoRepository;
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
 * 高性能 Redis Segment ID 生成器（双 buffer + 异步预加载 + 持久化 debounced）
 * <p>
 * 关键点：
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

    // local cache for segments
    private final ConcurrentHashMap<String, SegmentPair> segmentCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // thread pools
    // loaderPool: fixed size to avoid过多并发 DB/Redis 操作；默认 CPU*2
    private final ExecutorService loaderPool;
    // scheduler for periodic tasks (persist)
    private final ScheduledExecutorService scheduler;
    // persist dirty flag
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

    // ctor
    public RedisSegmentIDGenImpl() {
        int cpu = Math.max(1, Runtime.getRuntime().availableProcessors());
        // loaderPool: allow some parallelism but avoid冲击 redis/db
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
        // load state from file quickly (best-effort)
        loadCacheFromFile();

        // schedule periodic persistence (debounced)
        scheduler.scheduleAtFixedRate(this::persistCacheIfDirty, DEFAULT_PERSIST_INTERVAL_SECONDS,
                DEFAULT_PERSIST_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // test redis connectivity in background (don't block startup)
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

    // periodic persist if something changed
    private void persistCacheIfDirty() {
        if (!dirty.getAndSet(false)) return;
        persistCacheToFile();
    }

    private synchronized void persistCacheToFile() {
        try {
            Map<String, SegmentSnapshot> snap = segmentCache.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().snapshot()));
            objectMapper.writeValue(new File(CACHE_FILE), snap);
            log.debug("Persisted {} segments to {}", snap.size(), CACHE_FILE);
        } catch (Throwable t) {
            log.error("Persist cache to file failed: {}", t.getMessage(), t);
            dirty.set(true); // mark dirty for retry
        }
    }

    @Override
    public Mono<IMetaId> get(String key) {
        return Mono.fromCallable(() -> getId(key));
    }

    @Override
    public IMetaId getId(String key) {
        // fast path get-or-create pair
        SegmentPair pair = segmentCache.computeIfAbsent(key, SegmentPair::new);
        long id = pair.nextId();
        // debounce persistence: mark dirty, actual write happens periodically
        dirty.set(true);
        return IMetaId.builder().longId(id).build();
    }

    // shutdown helper (optional)
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
    // Snapshot structure for persistence
    // ----------------------------
    @Data
    public static class SegmentSnapshot {
        private long currentStart;
        private long currentEnd;
        private long currentCursor; // next value to use
        private int currentStep;

        private Long nextStart;
        private Long nextEnd;
        private Integer nextStep;
    }

    /**
     * LocalSegment: 内存轻量、线程安全、无锁的号段实现
     */
    private static final class LocalSegment {
        static final long EXHAUSTED = Long.MIN_VALUE;

        final long start;
        final long end;
        final int step;
        final AtomicLong cursor; // next id to return

        LocalSegment(long start, long end, int step) {
            this.start = start;
            this.end = end;
            this.step = step;
            this.cursor = new AtomicLong(Math.max(start, start)); // next to return
        }

        // construct with known current cursor
        LocalSegment(long start, long end, int step, long currentCursor) {
            this.start = start;
            this.end = end;
            this.step = step;
            this.cursor = new AtomicLong(Math.max(currentCursor, start));
        }

        long next() {
            while (true) {
                long cur = cursor.get();
                if (cur > end) return EXHAUSTED;
                long next = cur;
                if (cursor.compareAndSet(cur, cur + 1)) {
                    return next;
                }
                // CAS 失败，重试（极少数）
            }
        }

        long remaining() {
            long cur = cursor.get();
            long rem = end - cur + 1;
            return Math.max(0, rem);
        }

        int getStep() {
            return this.step;
        }
    }

    // ----------------------------
    // SegmentPair and LocalSegment
    // ----------------------------
    private class SegmentPair {
        private final String key;

        // flags
        private final AtomicBoolean loading = new AtomicBoolean(false);

        // use volatile for visibility; LocalSegment is thread-safe
        private volatile LocalSegment current;
        private volatile LocalSegment nextSegment;

        SegmentPair(String key) {
            this.key = key;
            // load initial synchronously but on loaderPool to avoid blocking caller thread if DB/Redis slow
            Future<LocalSegment> f = loaderPool.submit(this::loadSegmentBlocking);
            try {
                this.current = f.get(3, TimeUnit.SECONDS); // small timeout for initial load
            } catch (Throwable t) {
                log.warn("[{}] initial load slow or failed, creating fallback empty segment", key);
                // fallback to an empty segment that will trigger async load on first access
                this.current = new LocalSegment(initialId + 1, initialId, defaultStep);
                triggerAsyncLoad(); // proactively load
            }
        }

        SegmentPair(String key, SegmentSnapshot snap) {
            this.key = key;
            this.current = new LocalSegment(snap.getCurrentStart(), snap.getCurrentEnd(), snap.getCurrentStep(), snap.getCurrentCursor());
            if (snap.getNextStart() != null) {
                this.nextSegment = new LocalSegment(snap.getNextStart(), snap.getNextEnd(), snap.getNextStep());
            }
        }


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
                    // 在临界区内再次确认并执行切换（double-check）
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

                // 轻量等待：短自旋 + 退避（避免 busy loop）
                retry++;
                if (retry > 200) { // 调整上限，可配置
                    throw new IllegalStateException("Segment exhausted and new segment not ready for key=" + key);
                }
                // parkNanos 做短暂停顿，避免调用线程完全忙等
                // 逐步增加等待时间以降低 CPU 占用（指数退避或线性退避都可）
                long backoffNanos = Math.min(1_000L * retry, 1_000_000L); // 最多 1ms
                LockSupport.parkNanos(backoffNanos);
                // 重试循环会再次检查 current/nextSegment
            }
        }

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
                                log.debug("[{}] nextSegment already present, discarding loaded segment {}-{}", key, seg.start, seg.end);
                            }
                        }
                        if (log.isDebugEnabled()) log.debug("[{}] async prefetch done: {}-{}", key, seg.start, seg.end);
                    } catch (Throwable t) {
                        log.error("[{}] async load failed", key, t);
                    } finally {
                        loading.set(false);
                    }
                });
            }
        }


        /**
         * blocking segment load - runs in loaderPool threads
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

                // 1. load meta from DB (sync)
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

                // 2. check redis current value (block here but running in loader thread)
                Object redisValObj = reactiveRedisTemplate.opsForValue().get(key).block(Duration.ofSeconds(2));
                if (redisValObj == null) {
                    // initialize redis with meta.maxId
                    reactiveRedisTemplate.opsForValue().set(key, meta.getMaxId()).block(Duration.ofSeconds(2));
                }

                // 3. increment redis to allocate new range
                Long newMax = reactiveRedisTemplate.opsForValue().increment(key, step).block(Duration.ofSeconds(3));
                if (newMax == null) {
                    throw new IllegalStateException("Redis increment returned null for key=" + key);
                }
                long start = newMax - step + 1;
                long end = newMax;

                // 4. persist meta.maxId asynchronously (do not block caller)
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

                // return new LocalSegment
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
