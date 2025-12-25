package com.xy.lucky.logging.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.config.LoggingProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

/**
 * 异步日志批量发送器
 * 使用内存队列聚合日志并周期性批量上报，最大限度降低业务线程开销
 */
@Slf4j
public class AsyncLogSender {
    private final LoggingProperties props;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<Map<String, Object>> queue;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private volatile boolean running = false;

    public AsyncLogSender(LoggingProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.queue = new ArrayBlockingQueue<>(Math.max(1, props.getQueueCapacity()));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "im-log-sender");
            t.setDaemon(true);
            return t;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 启动后台批量发送任务
     */
    public synchronized void start() {
        if (running) return;
        running = true;
        scheduler.scheduleAtFixedRate(this::flush, props.getFlushIntervalMs(), props.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
        log.info("AsyncLogSender started: batchSize={}, interval={}ms, queueCapacity={}",
                props.getBatchSize(), props.getFlushIntervalMs(), props.getQueueCapacity());
    }

    /**
     * 停止并尝试发送剩余日志
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            scheduler.shutdown();
            flush();
        } catch (Exception e) {
            log.warn("AsyncLogSender stop flush error: {}", e.getMessage(), e);
        }
    }

    /**
     * 入队日志，满了则丢弃并记录一次告警
     */
    public void offer(Map<String, Object> logRecord) {
        if (!running) return;
        if (!queue.offer(logRecord)) {
            log.warn("Log queue is full, dropping one log");
        }
    }

    /**
     * 批量提取并上报
     */
    void flush() {
        if (!running) return;
        try {
            List<Map<String, Object>> batch = drain(props.getBatchSize());
            if (batch.isEmpty()) return;
            sendBatch(batch);
        } catch (Exception e) {
            log.debug("Flush logs error: {}", e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> drain(int max) {
        List<Map<String, Object>> list = new ArrayList<>(max);
        queue.drainTo(list, Math.max(1, max));
        return list;
    }

    private void sendBatch(List<Map<String, Object>> batch) {
        try {
            byte[] payload = serialize(batch, props.isGzipEnabled());
            String url = buildUrl(props.getServerBaseUrl(), props.getIngestBatchPath());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(props.getReadTimeoutMs()))
                    .header("Content-Type", "application/json");
            if (props.isGzipEnabled()) {
                builder.header("Content-Encoding", "gzip");
            }
            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofByteArray(payload)).build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .orTimeout(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            log.debug("Send batch logs error: {}", err.getMessage(), err);
                        } else if (resp.statusCode() >= 300) {
                            log.debug("Send batch logs non-2xx: {}", resp.statusCode());
                        }
                    });
        } catch (Exception e) {
            log.debug("Send batch logs exception: {}", e.getMessage(), e);
        }
    }

    private byte[] serialize(Object obj, boolean gzip) throws JsonProcessingException {
        byte[] json = objectMapper.writeValueAsBytes(obj);
        if (!gzip) return json;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(json.length);
            try (OutputStream gout = new GZIPOutputStream(baos)) {
                gout.write(json);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return json;
        }
    }

    private String buildUrl(String base, String path) {
        Objects.requireNonNull(base, "serverBaseUrl required");
        Objects.requireNonNull(path, "ingestPath required");
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        } else if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }
}

