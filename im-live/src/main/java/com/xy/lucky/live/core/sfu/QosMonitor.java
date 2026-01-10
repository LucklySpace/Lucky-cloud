package com.xy.lucky.live.core.sfu;

import com.xy.lucky.live.core.RoomManager;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * QoS 质量监控器
 * <p>
 * 监控直播服务的质量指标，包括：
 * <ul>
 *   <li>信令延迟统计</li>
 *   <li>连接成功率</li>
 *   <li>流媒体质量</li>
 *   <li>系统负载</li>
 * </ul>
 *
 * <h2>监控指标</h2>
 * <pre>
 * ┌─────────────────┬────────────────┐
 * │ 指标类别         │ 具体指标        │
 * ├─────────────────┼────────────────┤
 * │ 信令延迟         │ 平均/最大/P99  │
 * │ 连接质量         │ 成功率/失败率   │
 * │ 负载统计         │ QPS/并发数     │
 * │ 资源使用         │ 内存/线程      │
 * └─────────────────┴────────────────┘
 * </pre>
 *
 * @author lucky
 * @version 1.0.0
 */
@Component
public class QosMonitor implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(QosMonitor.class);

    /**
     * 信令延迟样本窗口大小
     */
    private static final int LATENCY_SAMPLE_SIZE = 1000;

    /**
     * 信令延迟样本（环形缓冲区）
     */
    private final long[] latencySamples = new long[LATENCY_SAMPLE_SIZE];
    /**
     * 每秒请求数统计
     */
    private final LongAdder requestsPerSecond = new LongAdder();
    private final AtomicLong lastSecondRequests = new AtomicLong(0);
    /**
     * 消息统计
     */
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong totalJoins = new AtomicLong(0);
    private final AtomicLong totalPublishes = new AtomicLong(0);
    private final AtomicLong totalSubscribes = new AtomicLong(0);
    /**
     * 错误统计
     */
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final Map<String, LongAdder> errorsByType = new ConcurrentHashMap<>();
    /**
     * 连接统计
     */
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong peakConnections = new AtomicLong(0);
    /**
     * 启动时间
     */
    private final long startTime = System.currentTimeMillis();
    private volatile int latencyIndex = 0;
    private volatile int latencyCount = 0;
    /**
     * 定时任务调度器
     */
    private ScheduledExecutorService scheduler;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private MediaForwarder mediaForwarder;

    @Autowired
    private ConnectionManager connectionManager;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 创建虚拟线程调度器
        scheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual()
                .name("qos-monitor-", 0)
                .factory());

        // 每秒重置 QPS 计数
        scheduler.scheduleAtFixedRate(this::resetQpsCounter, 1, 1, TimeUnit.SECONDS);

        // 每 5 分钟输出监控报告
        scheduler.scheduleAtFixedRate(this::logMonitoringReport, 5, 5, TimeUnit.MINUTES);

        log.info("QosMonitor 初始化完成");
    }

    /**
     * 记录信令延迟
     *
     * @param latencyMs 延迟（毫秒）
     */
    public void recordLatency(long latencyMs) {
        synchronized (latencySamples) {
            latencySamples[latencyIndex] = latencyMs;
            latencyIndex = (latencyIndex + 1) % LATENCY_SAMPLE_SIZE;
            if (latencyCount < LATENCY_SAMPLE_SIZE) {
                latencyCount++;
            }
        }
    }

    /**
     * 记录请求
     */
    public void recordRequest() {
        requestsPerSecond.increment();
        totalMessages.incrementAndGet();
    }

    /**
     * 记录加入房间
     */
    public void recordJoin() {
        totalJoins.incrementAndGet();
        recordRequest();
    }

    /**
     * 记录发布流
     */
    public void recordPublish() {
        totalPublishes.incrementAndGet();
        recordRequest();
    }

    /**
     * 记录订阅流
     */
    public void recordSubscribe() {
        totalSubscribes.incrementAndGet();
        recordRequest();
    }

    /**
     * 记录错误
     *
     * @param errorType 错误类型
     */
    public void recordError(String errorType) {
        totalErrors.incrementAndGet();
        errorsByType.computeIfAbsent(errorType, k -> new LongAdder()).increment();
    }

    /**
     * 记录连接打开
     */
    public void recordConnectionOpen() {
        totalConnections.incrementAndGet();
        long current = activeConnections.incrementAndGet();
        peakConnections.updateAndGet(peak -> Math.max(peak, current));
    }

    /**
     * 记录连接关闭
     */
    public void recordConnectionClose() {
        activeConnections.decrementAndGet();
    }

    /**
     * 获取平均延迟
     *
     * @return 平均延迟（毫秒）
     */
    public double getAverageLatency() {
        if (latencyCount == 0) return 0;
        synchronized (latencySamples) {
            long sum = 0;
            for (int i = 0; i < latencyCount; i++) {
                sum += latencySamples[i];
            }
            return (double) sum / latencyCount;
        }
    }

    /**
     * 获取最大延迟
     *
     * @return 最大延迟（毫秒）
     */
    public long getMaxLatency() {
        if (latencyCount == 0) return 0;
        synchronized (latencySamples) {
            long max = 0;
            for (int i = 0; i < latencyCount; i++) {
                max = Math.max(max, latencySamples[i]);
            }
            return max;
        }
    }

    /**
     * 获取 P99 延迟
     *
     * @return P99 延迟（毫秒）
     */
    public long getP99Latency() {
        if (latencyCount == 0) return 0;
        synchronized (latencySamples) {
            long[] sorted = new long[latencyCount];
            System.arraycopy(latencySamples, 0, sorted, 0, latencyCount);
            java.util.Arrays.sort(sorted);
            int index = (int) Math.ceil(latencyCount * 0.99) - 1;
            return sorted[Math.max(0, index)];
        }
    }

    /**
     * 获取当前 QPS
     *
     * @return QPS
     */
    public long getQps() {
        return lastSecondRequests.get();
    }

    /**
     * 获取运行时长（秒）
     *
     * @return 运行时长
     */
    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * 获取完整统计信息
     *
     * @return 统计信息 Map
     */
    public Map<String, Object> getStats() {
        return Map.ofEntries(
                // 延迟统计
                Map.entry("latencyAvg", String.format("%.2f ms", getAverageLatency())),
                Map.entry("latencyMax", getMaxLatency() + " ms"),
                Map.entry("latencyP99", getP99Latency() + " ms"),

                // QPS 统计
                Map.entry("qps", getQps()),
                Map.entry("totalMessages", totalMessages.get()),

                // 业务统计
                Map.entry("totalJoins", totalJoins.get()),
                Map.entry("totalPublishes", totalPublishes.get()),
                Map.entry("totalSubscribes", totalSubscribes.get()),

                // 错误统计
                Map.entry("totalErrors", totalErrors.get()),

                // 连接统计
                Map.entry("totalConnections", totalConnections.get()),
                Map.entry("activeConnections", activeConnections.get()),
                Map.entry("peakConnections", peakConnections.get()),

                // 房间统计
                Map.entry("activeRooms", roomManager.getRoomCount()),
                Map.entry("totalUsers", roomManager.getTotalUserCount()),

                // 转发统计
                Map.entry("forwardingSessions", mediaForwarder.getSessionCount()),
                Map.entry("totalSubscribers", mediaForwarder.getTotalSubscriberCount()),

                // 系统信息
                Map.entry("uptimeSeconds", getUptimeSeconds()),
                Map.entry("startTime", startTime)
        );
    }

    /**
     * 获取系统健康状态
     *
     * @return 健康状态
     */
    public HealthStatus getHealthStatus() {
        double avgLatency = getAverageLatency();
        long errors = totalErrors.get();
        int activeRooms = roomManager.getRoomCount();

        // 简单的健康判断逻辑
        if (avgLatency > 1000 || errors > 100) {
            return HealthStatus.UNHEALTHY;
        } else if (avgLatency > 500 || errors > 50) {
            return HealthStatus.DEGRADED;
        }
        return HealthStatus.HEALTHY;
    }

    /**
     * 重置 QPS 计数器（每秒调用）
     */
    private void resetQpsCounter() {
        lastSecondRequests.set(requestsPerSecond.sumThenReset());
    }

    /**
     * 输出监控报告
     */
    private void logMonitoringReport() {
        Map<String, Object> stats = getStats();
        HealthStatus health = getHealthStatus();

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    QoS 监控报告                               ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  健康状态: {}", health);
        log.info("║  运行时长: {}s", stats.get("uptimeSeconds"));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  延迟统计:");
        log.info("║    平均: {}, 最大: {}, P99: {}",
                stats.get("latencyAvg"), stats.get("latencyMax"), stats.get("latencyP99"));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  流量统计:");
        log.info("║    QPS: {}, 总消息: {}",
                stats.get("qps"), stats.get("totalMessages"));
        log.info("║    加入: {}, 发布: {}, 订阅: {}",
                stats.get("totalJoins"), stats.get("totalPublishes"), stats.get("totalSubscribes"));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  连接统计:");
        log.info("║    活跃: {}, 峰值: {}, 总计: {}",
                stats.get("activeConnections"), stats.get("peakConnections"), stats.get("totalConnections"));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  房间统计:");
        log.info("║    房间数: {}, 用户数: {}, 转发会话: {}",
                stats.get("activeRooms"), stats.get("totalUsers"), stats.get("forwardingSessions"));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  错误统计: {}", stats.get("totalErrors"));
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    /**
     * 销毁
     */
    @Override
    @PreDestroy
    public void destroy() {
        log.info("QosMonitor 正在关闭...");

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 输出最终报告
        logMonitoringReport();
        log.info("QosMonitor 已关闭");
    }

    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        /**
         * 健康
         */
        HEALTHY,

        /**
         * 性能下降
         */
        DEGRADED,

        /**
         * 不健康
         */
        UNHEALTHY
    }
}

