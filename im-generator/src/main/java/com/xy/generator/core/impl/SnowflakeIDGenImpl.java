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
 * 高性能无锁 Snowflake 实现
 * - 状态通过单个 AtomicLong 打包 (timestamp << sequenceBits) | sequence
 * - CAS 循环保证并发安全，无 synchronized
 * - workerId 仅首次加载且校验
 */
@Slf4j
@Component("snowflakeIDGen")
public class SnowflakeIDGenImpl implements IDGen {

    // epoch (ms)
    private static final long EPOCH = 1622505600000L; // 2021-06-01T00:00:00Z

    // bit allocation
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    // masks and shifts
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // spin/wait tunables
    private static final long MAX_CLOCK_BACK_MS = 5_000L; // 5s
    private static final int SPIN_YIELDS_BEFORE_PARK = 16;
    // state: [timestamp(ms) << SEQUENCE_BITS] | sequence
    private final AtomicLong state = new AtomicLong(0L); // initial lastTs = 0, seq = 0
    // workerId lazy init
    private final AtomicLong workerId = new AtomicLong(-1L); // -1 => not loaded
    private final AtomicBoolean workerLoaded = new AtomicBoolean(false);
    @Resource
    private WorkerIdAssigner workerIdAssigner;

    @Override
    public boolean init() {
        // no blocking init required; workerId lazy-load on first request
        return true;
    }

    @Override
    public Mono<IMetaId> get(String key) {
        // preserve Reactor style; core generation is lock-free and quick
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
     */
    private long nextIdInternal() {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();

        while (true) {
            long currentState = state.get();
            long lastTs = currentState >>> SEQUENCE_BITS;
            long seq = currentState & SEQUENCE_MASK;

            long now = timeGen();

            if (now < lastTs) {
                // 时钟回拨：小幅度 -> 等待；大幅度 -> 抛出
                long offset = lastTs - now;
                if (offset > MAX_CLOCK_BACK_MS) {
                    // 严重回拨
                    log.error("clock moved backwards too much: {} ms", offset);
                    throw new IllegalStateException("Clock moved backwards by " + offset + "ms");
                }
                // 小幅回拨：短自旋等待恢复（带少量park）
                now = waitUntilAtLeast(lastTs);
                // loop continue to attempt CAS
                continue;
            }

            if (now == lastTs) {
                // 同一毫秒，序列增加
                if (seq >= SEQUENCE_MASK) {
                    // 序列耗尽，等待下一毫秒
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
                LockSupport.parkNanos(1_000L); // 1 microsecond
            }
            ts = timeGen();
        }
        return ts;
    }

    /**
     * wait until system time >= targetTimestamp
     * used when clock rolled back slightly
     */
    private long waitUntilAtLeast(long targetTimestamp) {
        long ts = timeGen();
        long spins = 0;
        while (ts < targetTimestamp) {
            spins++;
            onSpinWaitHint();
            if ((spins & 0xFF) == 0) {
                // occasional short park to avoid burning CPU for long backoffs
                LockSupport.parkNanos(1_000L * Math.min(1000, spins)); // escalate up to ~1ms
            }
            ts = timeGen();
        }
        return ts;
    }

    private void onSpinWaitHint() {
        // Java 9+ hint for spin loops
        try {
            Thread.onSpinWait();
        } catch (Throwable ignored) {
            // fallback: no-op on older JDK
        }
    }

    private long composeId(long timestampMs, long seq) {
        long wId = workerId.get();
        return ((timestampMs - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | ((wId & MAX_WORKER_ID) << WORKER_ID_SHIFT)
                | (seq & SEQUENCE_MASK);
    }

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
            // call assigner to load worker id
            try {
                workerIdAssigner.load(); // assume id assigner handles its own blocking/IO
                long id = workerIdAssigner.getWorkerId();
                if (id < 0 || id > MAX_WORKER_ID) {
                    throw new IllegalArgumentException("workerId out of range: " + id);
                }
                workerId.set(id);
                workerLoaded.set(true);
                if (log.isInfoEnabled()) log.info("Snowflake workerId loaded: {}", id);
            } catch (Throwable t) {
                log.error("Failed to load workerId", t);
                throw new IllegalStateException("Failed to load workerId", t);
            }
        }
    }
}
