package com.xy.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 虚拟线程池
 *
 * @author dense
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean
    @Primary // 将我们设置的虚拟线程池作为主线程池
    public Executor virtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("im-server-virtual-thread-", 1).factory());
    }

    @Bean("asyncTaskExecutor")
    public TaskExecutor taskExecutor() {
        // 使用虚拟线程池
        return new TaskExecutor() {

            private final Executor executor = virtualThreadExecutor();

            @Override
            public void execute(Runnable task) {
                executor.execute(task);
            }

        };
    }


} 