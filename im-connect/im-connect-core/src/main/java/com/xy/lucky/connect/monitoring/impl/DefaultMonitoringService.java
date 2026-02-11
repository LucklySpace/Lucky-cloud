package com.xy.lucky.connect.monitoring.impl;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.monitoring.MonitoringService;
import com.xy.lucky.spring.annotations.core.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控服务默认实现
 * - 记录异常和性能指标
 * - 提供基本的内存计数器
 * - 可扩展为接入外部监控系统
 */
@Slf4j(topic = LogConstant.Monitoring)
@Component
public class DefaultMonitoringService implements MonitoringService {

    // 简单计数器，用于记录各类事件发生次数
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public void reportCritical(String context, Exception ex) {
        log.error("[严重异常] 来源: {}, 类型: {}, 消息: {}", context, ex.getClass().getSimpleName(), ex.getMessage());
        incrementCounter("exception.critical", "context:" + context, "type:" + ex.getClass().getSimpleName());
        // TODO: 接入外部告警系统，如短信、邮件通知
    }

    @Override
    public void reportError(String context, Exception ex) {
        log.error("[错误] 来源: {}, 类型: {}, 消息: {}", context, ex.getClass().getSimpleName(), ex.getMessage());
        incrementCounter("exception.error", "context:" + context, "type:" + ex.getClass().getSimpleName());
    }

    @Override
    public void reportWarning(String context, String message, Exception ex) {
        log.warn("[警告] 来源: {}, 消息: {}, 异常: {}", context, message, ex.getMessage());
        incrementCounter("exception.warning", "context:" + context);
    }

    @Override
    public void recordMetric(String name, double value, String... tags) {
        String key = formatMetricKey(name, tags);
        log.debug("记录指标: {} = {}", key, value);
        // TODO: 接入外部指标系统，如Prometheus
    }

    @Override
    public void incrementCounter(String name, String... tags) {
        String key = formatMetricKey(name, tags);
        counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        log.debug("计数器增加: {}", key);
    }

    @Override
    public void recordTime(String name, long timeInMs, String... tags) {
        String key = formatMetricKey(name, tags);
        log.debug("记录耗时: {} = {}ms", key, timeInMs);
        // TODO: 接入外部指标系统，如Prometheus
    }

    /**
     * 获取当前计数器值
     */
    public long getCounterValue(String name, String... tags) {
        String key = formatMetricKey(name, tags);
        AtomicLong counter = counters.get(key);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 格式化指标键名
     */
    private String formatMetricKey(String name, String... tags) {
        if (tags == null || tags.length == 0) {
            return name;
        }
        return name + ":" + String.join(",", Arrays.asList(tags));
    }
}