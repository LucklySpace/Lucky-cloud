package com.xy.lucky.live.core.sfu;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebRTC 连接管理器
 * <p>
 * 管理所有 WebRTC 连接的生命周期和状态。
 * 在 SFU 模式下，负责协调推流者和订阅者之间的连接建立过程。
 *
 * <h2>连接生命周期</h2>
 * <pre>
 * 新建(new) → 连接中(connecting) → 已连接(connected) → 断开(disconnected/failed)
 * </pre>
 *
 * <h2>功能</h2>
 * <ul>
 *   <li>跟踪所有 WebRTC 连接状态</li>
 *   <li>管理 ICE Candidate 缓存（处理 Trickle ICE）</li>
 *   <li>监控连接健康状态</li>
 *   <li>自动清理超时连接</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 */
@Component
public class ConnectionManager implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    /**
     * 连接超时时间（毫秒）- 连接建立超过此时间未完成视为超时
     */
    private static final long CONNECTION_TIMEOUT = 30000;

    /**
     * 连接 ID -> 连接信息映射
     * 连接 ID 格式: {publisherId}_{subscriberId}_{streamId}
     */
    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();

    /**
     * ICE Candidate 缓存
     * 连接 ID -> Candidate 队列
     * 用于处理 Trickle ICE 场景（Candidate 可能在 SDP 交换之前到达）
     */
    private final Map<String, Queue<Object>> candidateCache = new ConcurrentHashMap<>();

    /**
     * ICE Ufrag -> 连接 ID 映射
     * 用于通过 STUN 请求中的 USERNAME 属性快速查找连接
     */
    private final Map<String, String> ufragToConnectionId = new ConcurrentHashMap<>();

    /**
     * 统计：总连接数
     */
    private final AtomicLong totalConnections = new AtomicLong(0);

    /**
     * 统计：成功连接数
     */
    private final AtomicLong successfulConnections = new AtomicLong(0);

    /**
     * 统计：失败连接数
     */
    private final AtomicLong failedConnections = new AtomicLong(0);

    /**
     * 定时清理调度器
     */
    private ScheduledExecutorService cleanupScheduler;

    @Autowired
    private LiveProperties liveProperties;

    /**
     * 生成连接 ID
     *
     * @param publisherId  推流者 ID
     * @param subscriberId 订阅者 ID
     * @param streamId     流 ID
     * @return 连接 ID
     */
    public static String generateConnectionId(String publisherId, String subscriberId, String streamId) {
        return publisherId + "_" + subscriberId + "_" + streamId;
    }

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 创建虚拟线程调度器用于清理任务
        cleanupScheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual()
                .name("conn-cleanup-", 0)
                .factory());

        // 每 30 秒清理一次超时连接
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupTimeoutConnections,
                30, 30, TimeUnit.SECONDS
        );

        log.info("ConnectionManager 初始化完成");
    }

    /**
     * 创建连接
     *
     * @param publisherId       推流者 ID
     * @param subscriberId      订阅者 ID
     * @param streamId          流 ID
     * @param publisherChannel  推流者 Channel
     * @param subscriberChannel 订阅者 Channel
     * @return 连接信息
     */
    public ConnectionInfo createConnection(String publisherId, String subscriberId, String streamId,
                                           Channel publisherChannel, Channel subscriberChannel) {
        String connectionId = generateConnectionId(publisherId, subscriberId, streamId);

        ConnectionInfo info = new ConnectionInfo(
                connectionId, publisherId, subscriberId, streamId,
                publisherChannel, subscriberChannel
        );

        connections.put(connectionId, info);
        totalConnections.incrementAndGet();

        log.debug("创建连接: connectionId={}", connectionId);
        return info;
    }

    /**
     * 设置连接的 ICE 参数
     *
     * @param connectionId 连接 ID
     * @param localUfrag   本地（服务器）ICE 用户名片段
     * @param localPwd     本地（服务器）ICE 密码
     * @param remoteUfrag  远程（客户端）ICE 用户名片段
     */
    public void setIceCredentials(String connectionId, String localUfrag, String localPwd, String remoteUfrag) {
        ConnectionInfo info = connections.get(connectionId);
        if (info != null) {
            info.setLocalUfrag(localUfrag);
            info.setLocalPwd(localPwd);
            info.setRemoteUfrag(remoteUfrag);

            // 建立 ufrag 到连接 ID 的映射
            if (localUfrag != null) {
                ufragToConnectionId.put(localUfrag, connectionId);
            }
            if (remoteUfrag != null) {
                ufragToConnectionId.put(remoteUfrag, connectionId);
            }
        }
    }

    /**
     * 通过 ICE Ufrag 查找连接
     *
     * @param ufrag ICE 用户名片段
     * @return 连接信息，如果未找到返回 null
     */
    public ConnectionInfo getConnectionByUfrag(String ufrag) {
        String connectionId = ufragToConnectionId.get(ufrag);
        if (connectionId != null) {
            return connections.get(connectionId);
        }
        return null;
    }

    /**
     * 获取连接信息
     *
     * @param connectionId 连接 ID
     * @return 连接信息
     */
    public ConnectionInfo getConnection(String connectionId) {
        return connections.get(connectionId);
    }

    /**
     * 获取连接信息
     *
     * @param publisherId  推流者 ID
     * @param subscriberId 订阅者 ID
     * @param streamId     流 ID
     * @return 连接信息
     */
    public ConnectionInfo getConnection(String publisherId, String subscriberId, String streamId) {
        return connections.get(generateConnectionId(publisherId, subscriberId, streamId));
    }

    /**
     * 更新连接状态
     *
     * @param connectionId 连接 ID
     * @param state        新状态
     */
    public void updateConnectionState(String connectionId, String state) {
        ConnectionInfo info = connections.get(connectionId);
        if (info == null) return;

        String oldState = info.getState();
        info.setState(state);
        info.updateActiveTime();

        // 统计
        if ("connected".equals(state) && !"connected".equals(oldState)) {
            successfulConnections.incrementAndGet();
            log.info("连接建立成功: connectionId={}, duration={}ms", connectionId, info.getDuration());
        } else if ("failed".equals(state) || "closed".equals(state)) {
            if (!"connected".equals(oldState)) {
                failedConnections.incrementAndGet();
            }
            log.info("连接结束: connectionId={}, state={}, duration={}ms", connectionId, state, info.getDuration());
        }
    }

    /**
     * 更新 ICE 连接状态
     *
     * @param connectionId 连接 ID
     * @param state        ICE 状态
     */
    public void updateIceState(String connectionId, String state) {
        ConnectionInfo info = connections.get(connectionId);
        if (info != null) {
            info.setIceState(state);
            info.updateActiveTime();
        }
    }

    /**
     * 缓存 ICE Candidate
     * <p>
     * 在 SDP 交换完成之前收到的 Candidate 会被缓存
     *
     * @param connectionId 连接 ID
     * @param candidate    Candidate 信息
     */
    public void cacheCandidate(String connectionId, Object candidate) {
        candidateCache.computeIfAbsent(connectionId, k -> new ConcurrentLinkedQueue<>())
                .offer(candidate);
    }

    /**
     * 获取缓存的 Candidates
     *
     * @param connectionId 连接 ID
     * @return Candidate 列表
     */
    public List<Object> getCachedCandidates(String connectionId) {
        Queue<Object> queue = candidateCache.remove(connectionId);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(queue);
    }

    /**
     * 增加 Candidate 计数
     *
     * @param connectionId 连接 ID
     */
    public void incrementCandidateCount(String connectionId) {
        ConnectionInfo info = connections.get(connectionId);
        if (info != null) {
            info.incrementCandidateCount();
        }
    }

    /**
     * 标记 SDP 交换完成
     *
     * @param connectionId 连接 ID
     */
    public void markSdpExchanged(String connectionId) {
        ConnectionInfo info = connections.get(connectionId);
        if (info != null) {
            info.setSdpExchanged(true);
            info.updateActiveTime();
        }
    }

    /**
     * 移除连接
     *
     * @param connectionId 连接 ID
     * @return 被移除的连接信息
     */
    public ConnectionInfo removeConnection(String connectionId) {
        candidateCache.remove(connectionId);
        ConnectionInfo info = connections.remove(connectionId);
        if (info != null) {
            // 清理 ufrag 映射
            if (info.getLocalUfrag() != null) {
                ufragToConnectionId.remove(info.getLocalUfrag());
            }
            if (info.getRemoteUfrag() != null) {
                ufragToConnectionId.remove(info.getRemoteUfrag());
            }
            log.debug("移除连接: connectionId={}", connectionId);
        }
        return info;
    }

    /**
     * 移除指定流的所有连接
     *
     * @param streamId 流 ID
     * @return 被移除的连接列表
     */
    public List<ConnectionInfo> removeConnectionsByStream(String streamId) {
        List<ConnectionInfo> removed = new ArrayList<>();
        Iterator<Map.Entry<String, ConnectionInfo>> it = connections.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConnectionInfo> entry = it.next();
            if (entry.getValue().getStreamId().equals(streamId)) {
                removed.add(entry.getValue());
                candidateCache.remove(entry.getKey());
                it.remove();
            }
        }
        return removed;
    }

    /**
     * 移除指定用户的所有连接
     *
     * @param userId 用户 ID
     * @return 被移除的连接列表
     */
    public List<ConnectionInfo> removeConnectionsByUser(String userId) {
        List<ConnectionInfo> removed = new ArrayList<>();
        Iterator<Map.Entry<String, ConnectionInfo>> it = connections.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConnectionInfo> entry = it.next();
            ConnectionInfo info = entry.getValue();
            if (info.getPublisherId().equals(userId) || info.getSubscriberId().equals(userId)) {
                removed.add(info);
                candidateCache.remove(entry.getKey());
                it.remove();
            }
        }
        return removed;
    }

    /**
     * 获取活跃连接数
     *
     * @return 连接数
     */
    public int getActiveConnectionCount() {
        return connections.size();
    }

    /**
     * 获取健康连接数
     *
     * @return 健康连接数
     */
    public int getHealthyConnectionCount() {
        return (int) connections.values().stream()
                .filter(ConnectionInfo::isHealthy)
                .count();
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        int active = getActiveConnectionCount();
        int healthy = getHealthyConnectionCount();
        return Map.of(
                "activeConnections", active,
                "healthyConnections", healthy,
                "totalConnections", totalConnections.get(),
                "successfulConnections", successfulConnections.get(),
                "failedConnections", failedConnections.get(),
                "successRate", totalConnections.get() > 0 ?
                        String.format("%.2f%%", successfulConnections.get() * 100.0 / totalConnections.get()) : "N/A"
        );
    }

    /**
     * 清理超时连接
     */
    private void cleanupTimeoutConnections() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, ConnectionInfo> entry : connections.entrySet()) {
            ConnectionInfo info = entry.getValue();
            // 连接超时且未成功建立
            if (!info.isConnected() && info.getDuration() > CONNECTION_TIMEOUT) {
                toRemove.add(entry.getKey());
            }
        }

        for (String connectionId : toRemove) {
            ConnectionInfo info = removeConnection(connectionId);
            if (info != null) {
                failedConnections.incrementAndGet();
                log.warn("清理超时连接: connectionId={}, duration={}ms", connectionId, info.getDuration());
            }
        }
    }

    /**
     * 销毁
     */
    @Override
    @PreDestroy
    public void destroy() {
        log.info("ConnectionManager 正在关闭...");

        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        connections.clear();
        candidateCache.clear();
        ufragToConnectionId.clear();
        log.info("ConnectionManager 已关闭");
    }

    /**
     * 连接信息内部类
     */
    @lombok.Data
    public static class ConnectionInfo {

        private final String connectionId;
        private final String publisherId;
        private final String subscriberId;
        private final String streamId;
        private final Channel publisherChannel;
        private final Channel subscriberChannel;
        private final long createTime;

        private volatile String state = "new";
        private volatile String iceState = "new";
        private volatile boolean sdpExchanged = false;
        private volatile int candidateCount = 0;
        private volatile long lastActiveTime;

        // ICE 相关字段
        private volatile String localUfrag;      // 服务器 ICE 用户名片段
        private volatile String localPwd;       // 服务器 ICE 密码
        private volatile String remoteUfrag;     // 客户端 ICE 用户名片段
        private volatile java.net.InetSocketAddress remoteAddress; // 客户端 UDP 地址

        public ConnectionInfo(String connectionId, String publisherId, String subscriberId,
                              String streamId, Channel publisherChannel, Channel subscriberChannel) {
            this.connectionId = connectionId;
            this.publisherId = publisherId;
            this.subscriberId = subscriberId;
            this.streamId = streamId;
            this.publisherChannel = publisherChannel;
            this.subscriberChannel = subscriberChannel;
            this.createTime = System.currentTimeMillis();
            this.lastActiveTime = this.createTime;
        }

        public void updateActiveTime() {
            this.lastActiveTime = System.currentTimeMillis();
        }

        public void incrementCandidateCount() {
            this.candidateCount++;
            updateActiveTime();
        }

        public long getDuration() {
            return System.currentTimeMillis() - createTime;
        }

        public boolean isConnected() {
            return "connected".equals(state);
        }

        public boolean isHealthy() {
            return "connected".equals(state) || "connecting".equals(state);
        }

        public Map<String, Object> toSnapshot() {
            return Map.of(
                    "connectionId", connectionId,
                    "publisherId", publisherId,
                    "subscriberId", subscriberId,
                    "streamId", streamId,
                    "state", state,
                    "iceState", iceState,
                    "sdpExchanged", sdpExchanged,
                    "candidateCount", candidateCount,
                    "duration", getDuration(),
                    "createTime", createTime
            );
        }
    }
}

