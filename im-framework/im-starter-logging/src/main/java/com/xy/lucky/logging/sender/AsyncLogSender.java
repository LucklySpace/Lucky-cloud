package com.xy.lucky.logging.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.config.LoggingProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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

    public AsyncLogSender(LoggingProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.queue = new ArrayBlockingQueue<>(props.getQueueCapacity());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "im-log-sender");
            t.setDaemon(true);
            return t;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .build();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::flush, props.getFlushIntervalMs(), props.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        flush();
    }

    public void offer(Map<String, Object> logRecord) {
        if (!queue.offer(logRecord)) {
            // 队列满直接丢弃，避免阻塞业务
        }
    }

    private void flush() {
        try {
            List<Map<String, Object>> batch = new ArrayList<>();
            queue.drainTo(batch, props.getBatchSize());
            if (batch.isEmpty()) return;

            String json = objectMapper.writeValueAsString(batch);
            String url = props.getServerBaseUrl().replaceAll("/+$", "") + props.getIngestBatchPath();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(props.getReadTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.debug("Log send error", e);
        }
    }
}


