package com.xy.generator.core.impl;

import com.xy.generator.core.IDGen;
import com.xy.generator.work.WorkerIdAssigner;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactor 风格的雪花算法 ID 生成器实现，基于 WebFlux 非阻塞。
 * <p>
 * 通过单线程调度器保证序列号和时间戳状态的线程安全，无需显式锁或 synchronized。
 * 支持小幅度时钟回拨容错和随机序列号起始以提升安全性。
 * </p>
 */
@Slf4j
@Component("snowflakeIDGen")
public class SnowflakeIDGenImpl implements IDGen<Long> {

    /**
     * 起始时间戳：2021-06-01 00:00:00 UTC 毫秒数，可避免 2038 年问题
     */
    private static final long EPOCH = 1622505600000L;
    /**
     * 随机数生成器，用于随机序列起始
     */
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    // ==================== 位数配置 ====================
    /**
     * workerId 所占位数，默认 10 位，最大支持 0-1023 节点
     */
    private final long workerIdBits = 10L;
    /**
     * 序列号所占位数，默认 12 位，每毫秒最多 4096 个 ID
     */
    private final long sequenceBits = 12L;
    /**
     * 最大 workerId 值
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);
    /**
     * 左移位数：序列号位数
     */
    private final long workerIdShift = sequenceBits;
    /**
     * 左移位数：序列号位数 + workerId 位数
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits;
    /**
     * 序列号掩码，用于循环取模
     */
    private final long sequenceMask = ~(-1L << sequenceBits);
    /**
     * 当前工作节点 ID
     */
    private final AtomicLong workerId = new AtomicLong(0);

    // ==================== 运行时状态，使用原子变量 ====================
    /**
     * 序列号
     */
    private final AtomicLong sequence = new AtomicLong(0);
    /**
     * 上次生成 ID 的时间戳
     */
    private final AtomicLong lastTimestamp = new AtomicLong(-1L);
    @Resource
    private WorkerIdAssigner workerIdAssigner;

    @Override
    public boolean init() {
        return true;
    }

    /**
     * 核心 API：生成 ID。
     * <p>在独立调度器上非阻塞执行：加载 workerId、时钟回拨处理、组合 ID。</p>
     *
     * @param key 业务键，可用于埋点或区分日志
     * @return Mono 包裹的 ID
     */
    @Override
    public Mono<Long> get(String key) {
        return Mono.defer(() -> {
            // 确保 workerId 已加载并校验合法
            loadWorkerId();

            long ts = timeGen();
            log.debug("[{}] 当前时间戳：{}，上次时间戳：{}", key, ts, lastTimestamp.get());

            // 处理时钟回拨
            return handleClockBack(ts)
                    // 生成最终 ID
                    .map(this::generateId)
                    .doOnNext(id -> log.info("[{}] 获取 ID：{}", key, id));
        });
    }

    /**
     * 处理系统时钟回拨：
     * - 小于 5 秒的回拨，将延迟两倍时间再重试获取时间
     * - 大于阈值，抛出异常终止
     *
     * @param timestamp 初始采样时间
     * @return Mono 包裹的校正后时间戳
     */
    private Mono<Long> handleClockBack(long timestamp) {
        long lastTs = lastTimestamp.get();
        if (timestamp >= lastTs) {
            return Mono.just(timestamp);
        }

        long offset = lastTs - timestamp;
        log.warn("检测到时钟回拨，偏移：{} ms，允许阈值：5000 ms", offset);
        if (offset <= 5_000) {
            // 小幅度回拨，非阻塞等待
            long delayMs = offset << 1;
            log.info("小幅度回拨：延迟 {} ms 后重试", delayMs);
            return Mono.delay(Duration.ofMillis(delayMs))
                    .map(ignored -> {
                        long now = timeGen();
                        if (now < lastTimestamp.get()) {
                            log.error("回拨等待后仍未恢复，当前：{}，上次：{}", now, lastTimestamp.get());
                            throw new IllegalStateException("系统时钟异常");
                        }
                        log.info("回拨已恢复，当前时间：{}", now);
                        return now;
                    });
        }

        log.error("严重时钟回拨：{} ms，超过阈值，终止生成", offset);
        return Mono.error(new IllegalStateException("系统时钟异常超过阈值"));
    }

    /**
     * 根据时间戳和序列号状态生成最终 ID，并更新状态。
     *
     * @param timestamp 用于生成的时间戳
     * @return 生成的雪花 ID
     */
    private long generateId(long timestamp) {
        long lastTs = lastTimestamp.get();
        long seq;
        if (timestamp == lastTs) {
            // 同一毫秒内自增序列
            seq = (sequence.incrementAndGet()) & sequenceMask;
            if (seq == 0) {
                // 序列用尽，切换到下一毫秒
                seq = RANDOM.nextInt(100);
                timestamp = tilNextMillis(lastTs);
                log.debug("序列用尽，切换到下一毫秒：{}，初始随机序列：{}", timestamp, seq);
            }
        } else {
            // 不同毫秒，随机起始序列
            seq = RANDOM.nextInt(100);
            log.debug("新毫秒：{}，随机序列初始化：{}", timestamp, seq);
        }

        // 更新状态
        sequence.set(seq);
        lastTimestamp.set(timestamp);

        // 组合 ID：时间段 | workerId | 序列号
        long id = ((timestamp - EPOCH) << timestampLeftShift)
                | (workerId.get() << workerIdShift)
                | seq;

        log.trace("组成 ID: 时间部分 [{}], workerId [{}], 序列 [{}] => {}",
                timestamp - EPOCH, workerId.get(), seq, id);

        return id;
    }

    /**
     * 阻塞式获取下一毫秒（仅在序列用尽时调用）
     *
     * @param lastTs 上次使用的时间戳
     * @return 新的时间戳（保证 > lastTs）
     */
    private long tilNextMillis(long lastTs) {
        long ts = timeGen();
        while (ts <= lastTs) {
            ts = timeGen();
        }
        log.trace("到达下一毫秒：{}", ts);
        return ts;
    }

    /**
     * 获取当前系统时间戳，单位毫秒，可在测试中重写
     */
    protected long timeGen() {
        long now = System.currentTimeMillis();
        log.trace("timeGen 调用，当前时间：{}", now);
        return now;
    }

    /**
     * 加载并校验 workerId，仅首次调用有效。
     */
    private void loadWorkerId() {
        if (workerId.get() == 0) {
            log.info("加载 workerId");
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
}
