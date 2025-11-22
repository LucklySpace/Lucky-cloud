package com.xy.lucky.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 虚拟线程池配置
 * - 简化TaskExecutor包装，直接返回Executor
 * - 添加未捕获异常处理器
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * 主虚拟线程池（@Primary，用于@Async默认）
     */
    @Bean
    @Primary
    public Executor virtualThreadExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
                .name("im-server-virtual-thread-", 1)
                .uncaughtExceptionHandler((thread, throwable) -> {
                    log.error("虚拟线程[{}]未捕获异常", thread.getName(), throwable);
                })
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * 异步任务执行器（@Async指定"asyncTaskExecutor"使用）
     */
    @Bean("asyncTaskExecutor")
    public TaskExecutor asyncTaskExecutor() {
        // 直接返回虚拟线程池，无需额外包装
        return new ConcurrentTaskExecutor(virtualThreadExecutor());
    }
}