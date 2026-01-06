package com.xy.lucky.auth.config;

import io.micrometer.core.instrument.MeterRegistry;
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

    @Bean
    public BufferingApplicationStartup applicationStartup() {
        return new BufferingApplicationStartup(2048);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady(ContextRefreshedEvent event) {
        long startupTime = System.currentTimeMillis() - startTime;
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        log.info("========================================");
        log.info("Application Startup Performance Baseline:");
        log.info("  Startup Time: {} ms", startupTime);
        log.info("  Heap Memory Used: {} MB", heapUsage.getUsed() / 1024 / 1024);
        log.info("  Heap Memory Committed: {} MB", heapUsage.getCommitted() / 1024 / 1024);
        log.info("  Heap Memory Max: {} MB", heapUsage.getMax() / 1024 / 1024);
        log.info("========================================");
        
        // Register metrics
        try {
            MeterRegistry registry = event.getApplicationContext().getBean(MeterRegistry.class);
            registry.gauge("application.startup.time.ms", startupTime);
            registry.gauge("application.startup.memory.heap.used.mb", heapUsage.getUsed() / 1024.0 / 1024.0);
        } catch (Exception e) {
            log.warn("Failed to register startup metrics: {}", e.getMessage());
        }
    }
}


