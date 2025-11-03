package com.xy.connect.monitoring;

/**
 * 监控服务接口
 * 提供异常监控、性能指标收集和告警功能
 */
public interface MonitoringService {

    /**
     * 报告严重异常 - 可能触发熔断或告警
     */
    void reportCritical(String context, Exception ex);

    /**
     * 报告一般错误
     */
    void reportError(String context, Exception ex);

    /**
     * 报告警告级别问题
     */
    void reportWarning(String context, String message, Exception ex);

    /**
     * 记录性能指标
     */
    void recordMetric(String name, double value, String... tags);

    /**
     * 记录计数器
     */
    void incrementCounter(String name, String... tags);

    /**
     * 记录耗时
     */
    void recordTime(String name, long timeInMs, String... tags);
}