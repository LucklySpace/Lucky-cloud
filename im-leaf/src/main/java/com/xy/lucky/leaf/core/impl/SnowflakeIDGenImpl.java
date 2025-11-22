package com.xy.lucky.leaf.core.impl;

import com.xy.lucky.core.model.IMetaId;
import com.xy.lucky.leaf.config.NacosSnowflakeWorkerIdAllocator;
import com.xy.lucky.leaf.core.IDGen;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 高性能无锁 Snowflake ID 生成器实现
 * <p>
 * 核心特性：
 * - 使用单个 AtomicLong 打包状态：(timestamp << SEQUENCE_BITS) | sequence，实现无锁 CAS 操作，确保高并发和高性能。
 * - 支持时钟回拨处理：小幅度回拨通过自旋等待恢复，大幅度回拨抛出异常，确保高可用。
 * - workerId 仅在首次加载时从 Nacos 获取并校验。
 * - 自旋等待优化：结合 busy-spin 和微 park，降低 CPU 占用，同时保持低延迟。
 * - ID 结构：41 位时间戳（从 2021-06-01 开始，可用约 69 年） + 10 位 workerId + 12 位序列号，支持每毫秒 4096 个 ID。
 */
@Slf4j
@Component("snowflakeIDGen")
public class SnowflakeIDGenImpl implements IDGen {

    // 起始时间戳（ms）：2021-06-01T00:00:00Z
    private static final long EPOCH = 1622505600000L;

    // 位分配：workerId 10 位（支持 1024 台机器），序列号 12 位（每 ms 支持 4096 ID）
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    // 最大值和掩码
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    // 位移量
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // 时钟回拨阈值：最大允许 5 秒回拨，超出抛异常
    private static final long MAX_CLOCK_BACK_MS = 5000L;

    // 状态原子变量：高位存储时间戳，低位存储序列号
    private final AtomicLong state = new AtomicLong(0L);

    // 机器 ID，从 Nacos 获取
    private final long workerId;

    @Autowired
    public SnowflakeIDGenImpl(NacosSnowflakeWorkerIdAllocator workerIdAllocator) {
        this.workerId = workerIdAllocator.getWorkerId();
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("Invalid workerId: " + workerId);
        }
    }

    @Override
    public boolean init() {
        // 初始化无需阻塞，workerId 已惰性加载
        return true;
    }

    @Override
    public Mono<IMetaId> get(String key) {
        // Reactor 风格：异步包装同步生成逻辑
        return Mono.fromCallable(() -> getId(key));
    }

    @Override
    public IMetaId getId(String key) {
        long nextId = nextId();
        return IMetaId.builder().longId(nextId).build();
    }

    /**
     * 生成下一个 ID 的核心逻辑，使用无锁 CAS 循环确保线程安全和高性能。
     *
     * @return 生成的 Snowflake ID
     */
    private long nextId() {
        while (true) {
            long currentState = state.get();
            long lastTs = currentState >>> SEQUENCE_BITS;
            long seq = currentState & SEQUENCE_MASK;

            long now = timeGen();

            if (now < lastTs) {
                // 时钟回拨处理
                long offset = lastTs - now;
                if (offset > MAX_CLOCK_BACK_MS) {
                    log.error("Clock moved backwards too much: {} ms", offset);
                    throw new IllegalStateException("Clock moved backwards by " + offset + " ms");
                }
                // 小回拨：等待恢复
                now = waitUntil(now, lastTs);
                continue;
            }

            long newSeq;
            long newTs = now;

            if (now == lastTs) {
                // 同一毫秒：递增序列号
                newSeq = seq + 1;
                if (newSeq > SEQUENCE_MASK) {
                    // 序列耗尽：等待下一毫秒
                    newTs = waitUntil(now, now + 1);
                    newSeq = 0; // 新毫秒从 0 开始序列
                }
            } else {
                // 新毫秒：重置序列号
                newSeq = 0;
            }

            long newState = (newTs << SEQUENCE_BITS) | newSeq;
            if (state.compareAndSet(currentState, newState)) {
                // CAS 成功：组合并返回 ID
                return composeId(newTs, newSeq);
            }
            // CAS 失败：重试循环
        }
    }

    /**
     * 等待直到当前时间 >= targetTs，使用优化自旋（busy-spin + onSpinWait + 微 park）以平衡延迟和 CPU 占用。
     *
     * @param currentTs 当前时间戳
     * @param targetTs  目标时间戳
     * @return 满足条件后的时间戳
     */
    private long waitUntil(long currentTs, long targetTs) {
        long ts = currentTs;
        int spins = 0;
        while (ts < targetTs) {
            spins++;
            Thread.onSpinWait(); // Java 9+ 自旋提示，降低 CPU 占用
            if ((spins & 0xF) == 0) {
                // 每 16 次自旋，短暂 park 1us
                LockSupport.parkNanos(1000L);
            }
            ts = timeGen();
        }
        return ts;
    }

    /**
     * 组合 ID：时间戳 + workerId + 序列号。
     *
     * @param timestampMs 时间戳（毫秒）
     * @param seq         序列号
     * @return 组合后的 64 位 ID
     */
    private long composeId(long timestampMs, long seq) {
        return ((timestampMs - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | seq;
    }

    /**
     * 获取当前系统时间戳（毫秒级）。
     *
     * @return 当前时间戳
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
}