package com.xy.lucky.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高性能线程池配置
 * <p>
 * 特性：
 * 1. 虚拟线程池（JDK 21+）用于 I/O 密集型任务
 * 2. 固定线程池用于 CPU 密集型任务
 * 3. 自定义拒绝策略和监控
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class ThreadPoolConfig {

    @Value("${thread-pool.cpu-core-size:0}")
    private int cpuCoreSize;

    @Value("${thread-pool.io-max-size:200}")
    private int ioMaxSize;

    /**
     * 主虚拟线程池（@Primary，用于 @Async 默认和 I/O 密集型任务）
     * <p>
     * 虚拟线程适合：
     * - 网络 I/O（Redis、Dubbo 调用）
     * - 数据库操作
     * - MQ 消息发送
     */
    @Bean
    @Primary
    public Executor virtualThreadExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
                .name("im-virtual-", 1)
                .uncaughtExceptionHandler((thread, throwable) -> {
                    log.error("虚拟线程[{}]未捕获异常: {}", thread.getName(), throwable.getMessage(), throwable);
                })
                .factory();

        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * 异步任务执行器（@Async 指定 "asyncTaskExecutor" 使用）
     */
    @Bean("asyncTaskExecutor")
    public TaskExecutor asyncTaskExecutor() {
        return new ConcurrentTaskExecutor(virtualThreadExecutor());
    }

    /**
     * CPU 密集型任务线程池
     * <p>
     * 适用于：
     * - 消息序列化/反序列化
     * - 签名计算
     * - 数据处理
     */
    @Bean("cpuTaskExecutor")
    public ExecutorService cpuTaskExecutor() {
        int coreSize = cpuCoreSize > 0 ? cpuCoreSize : Runtime.getRuntime().availableProcessors();
        int maxSize = coreSize * 2;

        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "im-cpu-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) ->
                    log.error("CPU线程[{}]未捕获异常: {}", thread.getName(), ex.getMessage(), ex));
            return t;
        };

        // 使用 CallerRunsPolicy 作为降级策略
        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 消息推送专用线程池
     * <p>
     * 特点：
     * - 有界队列防止内存溢出
     * - 自定义拒绝策略记录被拒绝的任务
     */
    @Bean("messagePushExecutor")
    public ExecutorService messagePushExecutor() {
        int coreSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxSize = ioMaxSize;

        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "im-push-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };

        // 自定义拒绝策略：记录日志并尝试执行
        RejectedExecutionHandler rejectedHandler = (r, executor) -> {
            log.warn("消息推送任务被拒绝，队列已满: queueSize={}, activeCount={}",
                    executor.getQueue().size(), executor.getActiveCount());
            // 尝试用调用者线程执行
            if (!executor.isShutdown()) {
                r.run();
            }
        };

        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000),
                threadFactory,
                rejectedHandler
        );
    }

    /**
     * 定时任务线程池
     */
    @Bean("scheduledExecutor")
    public ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "im-scheduled");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 低优先级任务线程池（用于非关键任务）
     */
    @Bean("lowPriorityExecutor")
    public ExecutorService lowPriorityExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "im-low-priority");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }
}
