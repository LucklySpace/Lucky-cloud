package com.xy.lucky.general.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Startup performance monitoring configuration
 * - Records startup time and memory usage
 * - Exposes metrics via Actuator
 */
@Slf4j
@Configuration
public class StartupMetricsConfig {

    private final long startTime = System.currentTimeMillis();

    private Boolean hasPrintLog = false;

    @Bean
    public BufferingApplicationStartup applicationStartup() {
        return new BufferingApplicationStartup(2048);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady(ContextRefreshedEvent event) {
        if (!hasPrintLog) {
            long startupTime = System.currentTimeMillis() - startTime;

            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

            log.info("========================================");
            log.info("IM Gateway Startup Performance:");
            log.info("  Startup Time: {} ms", startupTime);
            log.info("  Heap Memory Used: {} MB", heapUsage.getUsed() / 1024 / 1024);
            log.info("  Heap Memory Committed: {} MB", heapUsage.getCommitted() / 1024 / 1024);
            log.info("  Heap Memory Max: {} MB", heapUsage.getMax() / 1024 / 1024);
            log.info("========================================");

            hasPrintLog = true;
        }
    }
}


