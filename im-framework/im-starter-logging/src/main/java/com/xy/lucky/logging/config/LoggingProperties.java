package com.xy.lucky.logging.config;

/**
 * 日志上报配置
 * 通过应用配置项控制日志上报行为与性能参数
 */
public class LoggingProperties {
    /**
     * 是否启用日志上报
     */
    private boolean enabled = true;
    /**
     * 日志服务基础地址，如 http://im-logging:8080
     */
    private String serverBaseUrl = "http://localhost:8200";
    /**
     * 单条日志上报接口路径
     */
    private String ingestPath = "/api/logs";
    /**
     * 批量上报接口路径
     */
    private String ingestBatchPath = "/api/logs/batch";
    /**
     * 模块名，默认读取 spring.application.name
     */
    private String module;
    /**
     * 服务名，默认与模块名一致
     */
    private String service;
    /**
     * 是否自动附加到 root logger
     */
    private boolean attachToRootLogger = false;
    /**
     * 队列最大长度
     */
    private int queueCapacity = 8192;
    /**
     * 每次批量发送的最大条数
     */
    private int batchSize = 200;
    /**
     * 批量刷新间隔（毫秒）
     */
    private long flushIntervalMs = 500;
    /**
     * HTTP 连接超时（毫秒）
     */
    private long connectTimeoutMs = 2000;
    /**
     * HTTP 读取超时（毫秒）
     */
    private long readTimeoutMs = 3000;
    /**
     * 是否启用GZIP压缩
     */
    private boolean gzipEnabled = true;
    /**
     * 是否包含线程名
     */
    private boolean includeThread = true;
    /**
     * 本机地址（host:port），为空则自动探测
     */
    private String address;
    /**
     * MDC中的traceId键名
     */
    private String mdcTraceIdKey = "traceId";
    /**
     * MDC中的spanId键名
     */
    private String mdcSpanIdKey = "spanId";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public String getIngestPath() {
        return ingestPath;
    }

    public void setIngestPath(String ingestPath) {
        this.ingestPath = ingestPath;
    }

    public String getIngestBatchPath() {
        return ingestBatchPath;
    }

    public void setIngestBatchPath(String ingestBatchPath) {
        this.ingestBatchPath = ingestBatchPath;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public boolean isAttachToRootLogger() {
        return attachToRootLogger;
    }

    public void setAttachToRootLogger(boolean attachToRootLogger) {
        this.attachToRootLogger = attachToRootLogger;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isGzipEnabled() {
        return gzipEnabled;
    }

    public void setGzipEnabled(boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
    }

    public boolean isIncludeThread() {
        return includeThread;
    }

    public void setIncludeThread(boolean includeThread) {
        this.includeThread = includeThread;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMdcTraceIdKey() {
        return mdcTraceIdKey;
    }

    public void setMdcTraceIdKey(String mdcTraceIdKey) {
        this.mdcTraceIdKey = mdcTraceIdKey;
    }

    public String getMdcSpanIdKey() {
        return mdcSpanIdKey;
    }

    public void setMdcSpanIdKey(String mdcSpanIdKey) {
        this.mdcSpanIdKey = mdcSpanIdKey;
    }
}
