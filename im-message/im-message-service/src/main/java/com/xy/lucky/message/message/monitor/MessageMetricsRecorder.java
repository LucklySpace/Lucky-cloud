package com.xy.lucky.message.message.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class MessageMetricsRecorder {

    private final MeterRegistry meterRegistry;
    private final Counter dispatchTotalCounter;
    private final Counter dispatchSuccessCounter;
    private final Counter dispatchRetryCounter;
    private final Counter dispatchFailureCounter;
    private final Timer dispatchLatencyTimer;
    private final AtomicInteger onlineConnectionCount = new AtomicInteger(0);
    private volatile Supplier<Integer> backlogSupplier = () -> 0;

    public MessageMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.dispatchTotalCounter = Counter.builder("im_message_dispatch_total")
                .description("message dispatch total count")
                .register(meterRegistry);
        this.dispatchSuccessCounter = Counter.builder("im_message_dispatch_success_total")
                .description("message dispatch success count")
                .register(meterRegistry);
        this.dispatchRetryCounter = Counter.builder("im_message_dispatch_retry_total")
                .description("message dispatch retry count")
                .register(meterRegistry);
        this.dispatchFailureCounter = Counter.builder("im_message_dispatch_failure_total")
                .description("message dispatch failure count")
                .register(meterRegistry);
        this.dispatchLatencyTimer = Timer.builder("im_message_dispatch_latency")
                .description("message dispatch latency")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(meterRegistry);
        Gauge.builder("im_message_connection_count", onlineConnectionCount, AtomicInteger::get)
                .description("online connection count")
                .register(meterRegistry);
    }

    public void bindDispatchQueue(BlockingQueue<?> queue) {
        backlogSupplier = queue::size;
        Gauge.builder("im_message_consumer_backlog", queue, BlockingQueue::size)
                .description("message dispatch consumer backlog")
                .register(meterRegistry);
    }

    public void onDispatchCreated() {
        dispatchTotalCounter.increment();
    }

    public void onDispatchSuccess(Duration duration) {
        dispatchSuccessCounter.increment();
        dispatchLatencyTimer.record(duration);
    }

    public void onDispatchRetry() {
        dispatchRetryCounter.increment();
    }

    public void onDispatchFailed(Duration duration) {
        dispatchFailureCounter.increment();
        dispatchLatencyTimer.record(duration);
    }

    public void setOnlineConnectionCount(int count) {
        onlineConnectionCount.set(Math.max(0, count));
    }

    public double getDispatchTotal() {
        return dispatchTotalCounter.count();
    }

    public double getDispatchSuccess() {
        return dispatchSuccessCounter.count();
    }

    public double getDispatchRetry() {
        return dispatchRetryCounter.count();
    }

    public double getDispatchFailure() {
        return dispatchFailureCounter.count();
    }

    public int getConsumerBacklog() {
        return Math.max(0, backlogSupplier.get());
    }

    public int getOnlineConnectionCount() {
        return onlineConnectionCount.get();
    }

    public double getP99LatencyMs() {
        return Arrays.stream(dispatchLatencyTimer.takeSnapshot().percentileValues())
                .filter(v -> Math.abs(v.percentile() - 0.99) < 0.0001)
                .map(ValueAtPercentile::value)
                .findFirst()
                .orElse(0d);
    }

    public Timer getDispatchLatencyTimer() {
        return dispatchLatencyTimer;
    }
}
