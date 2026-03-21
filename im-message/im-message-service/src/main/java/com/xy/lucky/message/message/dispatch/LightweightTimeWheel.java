package com.xy.lucky.message.message.dispatch;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轻量级时间轮调度器。
 * <p>
 * 适用于高频短延迟任务（例如消息重试），避免大量独立定时任务带来的调度开销。
 * </p>
 */
@Slf4j
public class LightweightTimeWheel {

    private final long tickMs;
    private final int wheelSize;
    private final ScheduledExecutorService scheduler;
    private final Executor taskExecutor;
    private final List<ConcurrentLinkedQueue<TimeoutTask>> buckets;
    private final AtomicLong currentTick = new AtomicLong(0);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> tickerFuture;

    /**
     * 创建时间轮。
     *
     * @param tickMs      每个槽位代表的毫秒数
     * @param wheelSize   槽位数量，建议为 2 的幂
     * @param scheduler   驱动时间轮前进的调度线程池
     * @param taskExecutor 任务执行器，避免在时间轮线程中直接执行业务逻辑
     */
    public LightweightTimeWheel(long tickMs, int wheelSize, ScheduledExecutorService scheduler, Executor taskExecutor) {
        if (tickMs <= 0) {
            throw new IllegalArgumentException("tickMs must be greater than 0");
        }
        if (wheelSize <= 0) {
            throw new IllegalArgumentException("wheelSize must be greater than 0");
        }
        this.tickMs = tickMs;
        this.wheelSize = wheelSize;
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
        this.buckets = new ArrayList<>(wheelSize);
        for (int i = 0; i < wheelSize; i++) {
            this.buckets.add(new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * 启动时间轮。
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        tickerFuture = scheduler.scheduleAtFixedRate(this::onTick, tickMs, tickMs, TimeUnit.MILLISECONDS);
        log.info("时间轮启动完成: tickMs={}, wheelSize={}", tickMs, wheelSize);
    }

    /**
     * 停止时间轮并清理未执行任务。
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        ScheduledFuture<?> future = tickerFuture;
        if (future != null) {
            future.cancel(false);
        }
        for (ConcurrentLinkedQueue<TimeoutTask> bucket : buckets) {
            bucket.clear();
        }
        log.info("时间轮已停止");
    }

    /**
     * 提交一个延迟任务。
     *
     * @param task    待执行任务
     * @param delayMs 延迟毫秒
     */
    public void schedule(Runnable task, long delayMs) {
        if (!started.get()) {
            throw new IllegalStateException("time wheel not started");
        }
        if (task == null) {
            return;
        }
        long safeDelay = Math.max(0, delayMs);
        long ticks = Math.max(1, (safeDelay + tickMs - 1) / tickMs);
        long rounds = (ticks - 1) / wheelSize;
        long baseTick = currentTick.get();
        long targetTick = baseTick + ticks;
        int slot = (int) (targetTick % wheelSize);
        buckets.get(slot).offer(new TimeoutTask(task, rounds));
    }

    private void onTick() {
        if (!started.get()) {
            return;
        }
        long tick = currentTick.incrementAndGet();
        int slot = (int) (tick % wheelSize);
        ConcurrentLinkedQueue<TimeoutTask> bucket = buckets.get(slot);
        int size = bucket.size();
        for (int i = 0; i < size; i++) {
            TimeoutTask timeoutTask = bucket.poll();
            if (timeoutTask == null) {
                continue;
            }
            if (timeoutTask.rounds > 0) {
                timeoutTask.rounds--;
                bucket.offer(timeoutTask);
                continue;
            }
            taskExecutor.execute(() -> runSafely(timeoutTask.task));
        }
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("时间轮任务执行异常", e);
        }
    }

    private static class TimeoutTask {
        private final Runnable task;
        private long rounds;

        private TimeoutTask(Runnable task, long rounds) {
            this.task = task;
            this.rounds = rounds;
        }
    }
}
