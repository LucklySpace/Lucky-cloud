package com.xy.generator.core.impl;

import com.xy.generator.core.IDGen;
import com.xy.generator.model.IdRingBuffer;
import com.xy.generator.work.WorkerIdAssigner;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 基于 Snowflake 算法的高性能 UID 生成器实现类
 */
@Slf4j
@Component("uidIDGen")
public class UIDGenImpl implements IDGen<Long> {

    /**
     * 当前工作节点 ID
     */
    private final AtomicLong workerId = new AtomicLong(0);
    private final AtomicLong sequence = new AtomicLong(0L); // 当前序列号
    // 起始时间戳（通常为项目统一设置的时间）
    @Value("${uid.epoch:1577836800000}")
    private long epoch;
    // 时间戳部分所占位数
    @Value("${uid.time-bits:41}")
    private int timeBits;
    // 工作节点ID所占位数
    @Value("${uid.worker-bits:10}")
    private int workerBits;
    // 序列号部分所占位数
    @Value("${uid.sequence-bits:12}")
    private int sequenceBits;
    // RingBuffer 缓存区大小的位数（2^n）
    @Value("${uid.buffer-size-bits:10}")
    private int bufferSizeBits;
    // 缓存预填充的触发阈值比例（0.2 表示 20% 时触发填充）
    @Value("${uid.padding-factor:0.2}")
    private double paddingFactor;
    private long maxWorkerId; // 最大允许的 workerId
    private long maxSequence; // 最大允许的序列号
    private int timestampShift; // 时间戳向左移动的位数
    private int workerShift;    // workerId 向左移动的位数
    private volatile long lastTimestamp = -1L; // 上一次生成 ID 的时间戳

    private IdRingBuffer<Long> ringBuffer; // UID 缓存区
    private int bufferSize; // 实际缓存区大小

    @Resource
    private WorkerIdAssigner workerIdAssigner;


    /**
     * 初始化 UID 生成器逻辑（此处不再使用定时任务）
     */
    @Override
    public boolean init() {
        // 初始化阶段可选择填充一部分
//        fillBuffer();
        return true;
    }

    /**
     * 加载并校验 workerId，仅首次调用有效。
     */
    private void loadWorkerId() {
        if (workerId.get() == 0) {

            log.info("加载 workerId");

            // 计算最大值
            this.maxWorkerId = ~(-1L << workerBits);
            this.maxSequence = ~(-1L << sequenceBits);

            // 位移计算
            this.workerShift = sequenceBits;
            this.timestampShift = sequenceBits + workerBits;

            // 初始化 RingBuffer
            this.bufferSize = 1 << bufferSizeBits;
            this.ringBuffer = new IdRingBuffer<>(bufferSize);

            workerIdAssigner.load();

            long id = workerIdAssigner.getWorkerId();

            if (id < 0 || id > maxWorkerId) {
                log.error("非法 workerId:{}，必须在 0-{} 范围内", id, maxWorkerId);
                throw new IllegalArgumentException("workerId 必须介于 0-" + maxWorkerId + " 之间");
            }
            workerId.set(id);

            log.info("加载完成，workerId = {}", id);
        }
    }

    /**
     * 获取下一个 UID（异步方式）
     * 在每次获取时自动判断缓存区余量是否需要补充
     */
    @Override
    public Mono<Long> get(String key) {
        return Mono.fromCallable(() -> {

            loadWorkerId();

            if (ringBuffer.size() < (int) (bufferSize * paddingFactor)) {
                fillBuffer(); // 剩余不足阈值时填充
            }

            Long id = ringBuffer.take();

            log.info("[{}] 获取 ID：{}", key, id);

            return id;
        });
    }

    /**
     * 批量填充 UID 缓存区直到满为止
     */
    private synchronized void fillBuffer() {
        while (!ringBuffer.isFull()) {
            ringBuffer.put(nextId());
        }
    }

    /**
     * 生成下一个唯一 ID（Snowflake 核心逻辑）
     */
    private synchronized long nextId() {
        long timestamp = currentTime();

        // 如果系统时钟回拨，抛出异常
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 相同毫秒内
        if (timestamp == lastTimestamp) {
            long seq = (sequence.incrementAndGet()) & maxSequence;
            if (seq == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence.set(0L); // 时间改变，重置序列号
        }

        lastTimestamp = timestamp;
        long diff = timestamp - epoch;
        return (diff << timestampShift)
                | (workerId.get() << workerShift)
                | sequence.get();
    }

    /**
     * 等待直到下一个毫秒
     */
    private long waitNextMillis(long lastTs) {
        long ts;
        do {
            ts = currentTime();
        } while (ts <= lastTs);
        return ts;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    private long currentTime() {
        return Instant.now().toEpochMilli();
    }
}
