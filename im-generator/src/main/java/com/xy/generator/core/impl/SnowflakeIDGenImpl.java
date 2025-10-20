package com.xy.generator.core.impl;

import com.xy.core.model.IMetaId;
import com.xy.generator.core.IDGen;
import com.xy.generator.work.WorkerIdAssigner;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 高性能无锁 Snowflake ID生成器实现
 *
 * 特性：
 * - 状态通过单个 AtomicLong 打包 (timestamp << sequenceBits) | sequence
 * - CAS 循环保证并发安全，无 synchronized
 * - workerId 仅首次加载且校验
 * - 支持时钟回拨处理
 */
@Slf4j
@Component("snowflakeIDGen")
public class SnowflakeIDGenImpl implements IDGen {

    // epoch (ms) - 2021-06-01T00:00:00Z
    private static final long EPOCH = 1622505600000L;

    // 位分配
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    // 掩码和位移
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // 自旋/等待调优参数
    private static final long MAX_CLOCK_BACK_MS = 5_000L; // 5s

    // 状态: [timestamp(ms) << SEQUENCE_BITS] | sequence
    private final AtomicLong state = new AtomicLong(0L); // 初始 lastTs = 0, seq = 0

    // workerId 惰性初始化
    private final AtomicLong workerId = new AtomicLong(-1L); // -1 => 未加载
    private final AtomicBoolean workerLoaded = new AtomicBoolean(false);

    @Resource
    private WorkerIdAssigner workerIdAssigner;

    @Override
    public boolean init() {
        // 无需阻塞初始化；workerId 在首次请求时惰性加载
        return true;
    }

    @Override
    public Mono<IMetaId> get(String key) {
        // 保持 Reactor 风格；核心生成是无锁且快速的
        return Mono.fromCallable(() -> getId(key));
    }

    @Override
    public IMetaId getId(String key) {
        ensureWorkerIdLoaded();
        long nextId = nextIdInternal();
        return IMetaId.builder().longId(nextId).build();
    }

    /**
     * 无锁 CAS 主循环生成 ID
     * @return 生成的ID
     */
    private long nextIdInternal() {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();

        while (true) {
            long currentState = state.get();
            long lastTs = currentState >>> SEQUENCE_BITS;
            long seq = currentState & SEQUENCE_MASK;

            long now = timeGen();

            if (now < lastTs) {
                // 时钟回拨：小幅度 -> 等待；大幅度 -> 抛出异常
                long offset = lastTs - now;
                if (offset > MAX_CLOCK_BACK_MS) {
                    // 严重回拨
                    log.error("Clock moved backwards too much: {} ms", offset);
                    throw new IllegalStateException("Clock moved backwards by " + offset + "ms");
                }
                // 小幅回拨：短自旋等待恢复（带少量park）
                now = waitUntilAtLeast(lastTs);
                // 继续循环尝试 CAS
                continue;
            }

            if (now == lastTs) {
                // 同一毫秒内，序列号递增
                if (seq >= SEQUENCE_MASK) {
                    // 序列号耗尽，等待下一毫秒
                    long newTs = tilNextMillis(lastTs);
                    long newSeq = rnd.nextLong(0, 100); // 随机起始
                    long newState = (newTs << SEQUENCE_BITS) | (newSeq & SEQUENCE_MASK);
                    if (state.compareAndSet(currentState, newState)) {
                        return composeId(newTs, newSeq);
                    } else {
                        // CAS失败，重试
                        continue;
                    }
                } else {
                    long newSeq = seq + 1;
                    long newState = (lastTs << SEQUENCE_BITS) | (newSeq & SEQUENCE_MASK);
                    if (state.compareAndSet(currentState, newState)) {
                        return composeId(lastTs, newSeq);
                    } else {
                        continue;
                    }
                }
            } else { // now > lastTs
                // 新的毫秒，重置序列号
                long newSeq = rnd.nextLong(0, 100);
                long newState = (now << SEQUENCE_BITS) | (newSeq & SEQUENCE_MASK);
                if (state.compareAndSet(currentState, newState)) {
                    return composeId(now, newSeq);
                } else {
                    continue;
                }
            }
        }
    }

    /**
     * 在序列耗尽或需要等待下一毫秒时，快速等待直到 timestamp > lastTs
     * 使用 busy-spin + onSpinWait + occasional park，延迟低且 CPU 占用可控
     * @param lastTs 上一时间戳
     * @return 下一时间戳
     */
    private long tilNextMillis(long lastTs) {
        long ts = timeGen();
        int spins = 0;
        while (ts <= lastTs) {
            spins++;
            // 在热路径上使用 CPU 自旋（JDK9+会使用 onSpinWait hint）
            onSpinWaitHint();
            if ((spins & 0xF) == 0) {
                // 每 16 次自旋做短暂 park，降低 CPU 占用
                LockSupport.parkNanos(1_000L); // 1 微秒
            }
            ts = timeGen();
        }
        return ts;
    }

    /**
     * 等待直到系统时间 >= targetTimestamp
     * 用于时钟轻微回拨的情况
     * @param targetTimestamp 目标时间戳
     * @return 当前时间戳
     */
    private long waitUntilAtLeast(long targetTimestamp) {
        long ts = timeGen();
        long spins = 0;
        while (ts < targetTimestamp) {
            spins++;
            onSpinWaitHint();
            if ((spins & 0xFF) == 0) {
                // 偶尔短暂停 park 以避免长时间回退时消耗 CPU
                LockSupport.parkNanos(1_000L * Math.min(1000, spins)); // 最多 ~1ms
            }
            ts = timeGen();
        }
        return ts;
    }

    /**
     * 自旋等待提示
     * Java 9+ 的自旋循环提示
     */
    private void onSpinWaitHint() {
        try {
            Thread.onSpinWait();
        } catch (Throwable ignored) {
            // 降级处理：在旧版本 JDK 上无操作
        }
    }

    /**
     * 组合ID
     *
     * @param timestampMs 时间戳（毫秒）
     * @param seq         序列号
     * @return 组合后的ID
     */
    private long composeId(long timestampMs, long seq) {
        long wId = workerId.get();
        return ((timestampMs - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | ((wId & MAX_WORKER_ID) << WORKER_ID_SHIFT)
                | (seq & SEQUENCE_MASK);
    }

    /**
     * 获取当前时间
     * @return 当前时间戳（毫秒）
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * workerId 惰性加载（只在首次执行时同步一次）
     */
    private void ensureWorkerIdLoaded() {
        if (workerLoaded.get()) return;

        synchronized (this) {
            if (workerLoaded.get()) return;
            // 调用分配器加载 worker id
            try {
                workerIdAssigner.load(); // 假设 id 分配器处理自己的阻塞/IO
                long id = workerIdAssigner.getWorkerId();
                if (id < 0 || id > MAX_WORKER_ID) {
                    throw new IllegalArgumentException("workerId out of range: " + id);
                }
                workerId.set(id);
                workerLoaded.set(true);
                if (log.isInfoEnabled()) {
                    log.info("Snowflake workerId loaded: {}", id);
                }
            } catch (Throwable t) {
                log.error("Failed to load workerId", t);
                throw new IllegalStateException("Failed to load workerId", t);
            }
        }
    }
}