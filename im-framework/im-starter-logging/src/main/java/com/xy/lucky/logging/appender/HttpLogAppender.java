package com.xy.lucky.logging.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.config.LoggingProperties;
import com.xy.lucky.logging.sender.AsyncLogSender;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 将 Logback 日志事件转换为日志记录并异步上报
 */
public class HttpLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private LoggingProperties props;
    private AsyncLogSender sender;
    private String resolvedAddress;

    // XML 配置支持的可选属性（通过setter注入）
    private String serverBaseUrl;
    private String url;
    private String ingestPath;
    private String ingestBatchPath;
    private String module;
    private String service;
    private Integer queueCapacity;
    private Integer batchSize;
    private Long flushIntervalMs;
    private Long connectTimeoutMs;
    private Long readTimeoutMs;
    private Boolean gzipEnabled;
    private Boolean includeThread;
    private String address;
    private String mdcTraceIdKey;
    private String mdcSpanIdKey;
    private Boolean enabled;

    public HttpLogAppender() {
    }

    public HttpLogAppender(LoggingProperties props, AsyncLogSender sender) {
        this.props = props;
        this.sender = sender;
    }

    @Override
    public void start() {
        if (this.props == null) {
            LoggingProperties p = new LoggingProperties();
            if (enabled != null) p.setEnabled(enabled);
            if (url != null) p.setServerBaseUrl(url);
            if (serverBaseUrl != null) p.setServerBaseUrl(serverBaseUrl);
            if (ingestPath != null) p.setIngestPath(ingestPath);
            if (ingestBatchPath != null) p.setIngestBatchPath(ingestBatchPath);
            String appName = module;
            if (appName == null || appName.isBlank()) {
                appName = System.getProperty("spring.application.name");
            }
            if (appName == null || appName.isBlank()) {
                appName = System.getenv("SPRING_APPLICATION_NAME");
            }
            if (appName != null && !appName.isBlank()) {
                p.setModule(appName);
            } else if (module != null) {
                p.setModule(module);
            }
            if (service != null) p.setService(service);
            if (queueCapacity != null && queueCapacity > 0) p.setQueueCapacity(queueCapacity);
            if (batchSize != null && batchSize > 0) p.setBatchSize(batchSize);
            if (flushIntervalMs != null && flushIntervalMs > 0) p.setFlushIntervalMs(flushIntervalMs);
            if (connectTimeoutMs != null && connectTimeoutMs > 0) p.setConnectTimeoutMs(connectTimeoutMs);
            if (readTimeoutMs != null && readTimeoutMs > 0) p.setReadTimeoutMs(readTimeoutMs);
            if (gzipEnabled != null) p.setGzipEnabled(gzipEnabled);
            if (includeThread != null) p.setIncludeThread(includeThread);
            if (address != null) p.setAddress(address);
            if (mdcTraceIdKey != null) p.setMdcTraceIdKey(mdcTraceIdKey);
            if (mdcSpanIdKey != null) p.setMdcSpanIdKey(mdcSpanIdKey);
            this.props = p;
        }
        if (this.props.getService() == null || this.props.getService().isBlank()) {
            this.props.setService(this.props.getModule());
        }
        if (this.sender == null) {
            this.sender = new AsyncLogSender(this.props, new ObjectMapper());
            if (this.props.isEnabled()) {
                this.sender.start();
            }
        }
        super.start();
    }

    @Override
    public void stop() {
        try {
            if (this.sender != null) {
                this.sender.stop();
            }
        } finally {
            super.stop();
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (sender == null || !this.started || (props != null && !props.isEnabled())) return;
        Map<String, Object> record = toRecord(event);
        sender.offer(record);
    }

    private Map<String, Object> toRecord(ILoggingEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("level", toLevel(event.getLevel()));
        map.put("module", props.getModule());
        map.put("service", props.getService() != null ? props.getService() : props.getModule());
        map.put("address", resolveAddress());
        map.put("message", event.getFormattedMessage());
        if (props.isIncludeThread()) {
            map.put("thread", event.getThreadName());
        }
        String traceId = MDC.get(props.getMdcTraceIdKey());
        if (traceId != null) {
            map.put("traceId", traceId);
        }
        String spanId = MDC.get(props.getMdcSpanIdKey());
        if (spanId != null) {
            map.put("spanId", spanId);
        }
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            map.put("exception", ThrowableProxyUtil.asString(tp));
        }
        return map;
    }

    private String toLevel(Level level) {
        if (level == null) return "INFO";
        if (level.isGreaterOrEqual(Level.ERROR)) return "ERROR";
        if (level.isGreaterOrEqual(Level.WARN)) return "WARN";
        if (level.isGreaterOrEqual(Level.INFO)) return "INFO";
        return "DEBUG";
    }

    private String resolveAddress() {
        if (resolvedAddress != null) return resolvedAddress;
        if (props.getAddress() != null && !props.getAddress().isBlank()) {
            resolvedAddress = props.getAddress();
            return resolvedAddress;
        }
        try {
            resolvedAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            resolvedAddress = "unknown";
        }
        return resolvedAddress;
    }

    // setters for XML configuration
    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setIngestPath(String ingestPath) {
        this.ingestPath = ingestPath;
    }

    public void setIngestBatchPath(String ingestBatchPath) {
        this.ingestBatchPath = ingestBatchPath;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setQueueCapacity(Integer queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public void setFlushIntervalMs(Long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public void setConnectTimeoutMs(Long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public void setReadTimeoutMs(Long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public void setGzipEnabled(Boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
    }

    public void setIncludeThread(Boolean includeThread) {
        this.includeThread = includeThread;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setMdcTraceIdKey(String mdcTraceIdKey) {
        this.mdcTraceIdKey = mdcTraceIdKey;
    }

    public void setMdcSpanIdKey(String mdcSpanIdKey) {
        this.mdcSpanIdKey = mdcSpanIdKey;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
