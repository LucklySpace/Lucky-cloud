package com.xy.lucky.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志上报配置
 */
@Data
@ConfigurationProperties(prefix = "im.logging")
public class LoggingProperties {
    private boolean enabled = true;
    private String serverBaseUrl = "http://localhost:8200";
    private String ingestPath = "/api/logs";
    private String ingestBatchPath = "/api/logs/batch";
    private String module;
    private String service;
    private String env;
    private boolean attachToRootLogger = false;
    private int queueCapacity = 8192;
    private int batchSize = 200;
    private long flushIntervalMs = 500;
    private long connectTimeoutMs = 2000;
    private long readTimeoutMs = 3000;
    private boolean gzipEnabled = true;
    private boolean includeThread = true;
    private String address;
    private String mdcTraceIdKey = "traceId";
    private String mdcSpanIdKey = "spanId";
}
