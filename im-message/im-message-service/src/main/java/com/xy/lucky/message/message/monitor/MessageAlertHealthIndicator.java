package com.xy.lucky.message.message.monitor;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("messageAlertBaseline")
public class MessageAlertHealthIndicator implements HealthIndicator {

    private final MessageMetricsRecorder metricsRecorder;
    private final MessageAlertBaselineProperties baselineProperties;

    public MessageAlertHealthIndicator(MessageMetricsRecorder metricsRecorder,
                                       MessageAlertBaselineProperties baselineProperties) {
        this.metricsRecorder = metricsRecorder;
        this.baselineProperties = baselineProperties;
    }

    @Override
    public Health health() {
        double total = metricsRecorder.getDispatchTotal();
        double success = metricsRecorder.getDispatchSuccess();
        double retry = metricsRecorder.getDispatchRetry();
        double successRate = total == 0 ? 1.0 : success / total;
        double retryRate = total == 0 ? 0.0 : retry / total;
        double p99 = metricsRecorder.getP99LatencyMs();
        int backlog = metricsRecorder.getConsumerBacklog();
        int connections = metricsRecorder.getOnlineConnectionCount();

        boolean healthy = successRate >= baselineProperties.getMinSuccessRate()
                && retryRate <= baselineProperties.getMaxRetryRate()
                && p99 <= baselineProperties.getMaxP99Ms()
                && backlog <= baselineProperties.getMaxConsumerBacklog()
                && connections >= baselineProperties.getMinConnectionCount();

        Health.Builder builder = healthy ? Health.up() : Health.down();
        return builder
                .withDetail("connectionCount", connections)
                .withDetail("successRate", successRate)
                .withDetail("retryRate", retryRate)
                .withDetail("p99Ms", p99)
                .withDetail("consumerBacklog", backlog)
                .build();
    }
}
