package com.xy.lucky.live.core.sfu;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.core.RoomManager;
import com.xy.lucky.live.core.model.LiveRoom;
import com.xy.lucky.live.core.model.LiveStream;
import com.xy.lucky.live.core.model.LiveUser;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SFU 媒体转发管理器
 * <p>
 * 作为 Selective Forwarding Unit (SFU) 的核心组件，负责：
 * <ul>
 *   <li>管理媒体流的发布和订阅关系</li>
 *   <li>协调推流者和拉流者之间的信令交换</li>
 *   <li>维护流的转发路由表</li>
 *   <li>收集和统计流的 QoS 指标</li>
 * </ul>
 *
 * <h2>中心化流媒体架构</h2>
 * <pre>
 * 推流者A ─┐                     ┌─ 拉流者1
 *          │                     │
 * 推流者B ─┼─ [SFU 信令服务器] ─┼─ 拉流者2
 *          │                     │
 * 推流者C ─┘                     └─ 拉流者3
 * </pre>
 *
 * <h2>工作流程</h2>
 * <ol>
 *   <li>推流者发布流 → 服务器记录流信息 → 广播新流通知</li>
 *   <li>拉流者订阅流 → 服务器建立转发关系 → 协调 SDP/ICE 交换</li>
 *   <li>媒体流直接在端到端传输（通过 STUN/TURN 打洞）</li>
 *   <li>服务器持续监控连接质量和状态</li>
 * </ol>
 *
 * @author lucky
 * @version 1.0.0
 */
@Component
public class MediaForwarder implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MediaForwarder.class);

    /**
     * 流 ID -> 转发会话映射
     * 每个流对应一个转发会话，包含所有订阅者的连接信息
     */
    private final Map<String, ForwardingSession> forwardingSessions = new ConcurrentHashMap<>();

    /**
     * Channel -> 会话列表映射（用于快速查找某个连接关联的所有会话）
     */
    private final Map<Channel, Set<String>> channelSessions = new ConcurrentHashMap<>();

    /**
     * 统计：总转发字节数
     */
    private final AtomicLong totalForwardedBytes = new AtomicLong(0);

    /**
     * 统计：总转发消息数
     */
    private final AtomicLong totalForwardedMessages = new AtomicLong(0);

    /**
     * 定时统计调度器
     */
    private ScheduledExecutorService statsScheduler;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private LiveProperties liveProperties;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 创建虚拟线程调度器用于统计任务
        statsScheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual()
                .name("media-stats-", 0)
                .factory());

        // 每分钟输出统计信息
        statsScheduler.scheduleAtFixedRate(
                this::logStats,
                60, 60, TimeUnit.SECONDS
        );

        log.info("MediaForwarder 初始化完成");
    }

    /**
     * 创建转发会话
     * <p>
     * 当推流者发布新流时调用，创建对应的转发会话
     *
     * @param roomId      房间 ID
     * @param publisherId 推流者 ID
     * @param streamId    流 ID
     * @param stream      流信息
     * @return 创建的转发会话
     */
    public ForwardingSession createSession(String roomId, String publisherId, String streamId, LiveStream stream) {
        ForwardingSession session = new ForwardingSession(roomId, publisherId, streamId, stream);
        forwardingSessions.put(streamId, session);

        // 记录推流者的 Channel 关联
        LiveRoom room = roomManager.getRoom(roomId);
        if (room != null) {
            LiveUser publisher = room.getUser(publisherId);
            if (publisher != null) {
                if (publisher.getChannel() != null) {
                    channelSessions.computeIfAbsent(publisher.getChannel(), k -> ConcurrentHashMap.newKeySet())
                            .add(streamId);
                }
            }
        }

        log.info("创建转发会话: streamId={}, publisher={}", streamId, publisherId);
        return session;
    }

    /**
     * 获取转发会话
     *
     * @param streamId 流 ID
     * @return 转发会话，不存在则返回 null
     */
    public ForwardingSession getSession(String streamId) {
        return forwardingSessions.get(streamId);
    }

    /**
     * 添加订阅者到转发会话
     *
     * @param streamId     流 ID
     * @param subscriberId 订阅者 ID
     * @param channel      订阅者 Channel
     * @return 是否添加成功
     */
    public boolean addSubscriber(String streamId, String subscriberId, Channel channel) {
        ForwardingSession session = forwardingSessions.get(streamId);
        if (session == null) {
            return false;
        }

        session.addSubscriber(subscriberId, channel);
        channelSessions.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet())
                .add(streamId);

        log.debug("添加订阅者: streamId={}, subscriber={}", streamId, subscriberId);
        return true;
    }

    /**
     * 移除订阅者
     *
     * @param streamId     流 ID
     * @param subscriberId 订阅者 ID
     * @return 是否移除成功
     */
    public boolean removeSubscriber(String streamId, String subscriberId) {
        ForwardingSession session = forwardingSessions.get(streamId);
        if (session == null) {
            return false;
        }

        SubscriberInfo removed = session.removeSubscriber(subscriberId);
        if (removed != null) {
            Set<String> sessions = channelSessions.get(removed.getChannel());
            if (sessions != null) {
                sessions.remove(streamId);
            }
            log.debug("移除订阅者: streamId={}, subscriber={}", streamId, subscriberId);
            return true;
        }
        return false;
    }

    /**
     * 销毁转发会话
     *
     * @param streamId 流 ID
     * @return 被销毁的会话
     */
    public ForwardingSession destroySession(String streamId) {
        ForwardingSession session = forwardingSessions.remove(streamId);
        if (session != null) {
            // 清理所有订阅者的 Channel 关联
            for (SubscriberInfo subscriber : session.getSubscribers().values()) {
                Set<String> sessions = channelSessions.get(subscriber.getChannel());
                if (sessions != null) {
                    sessions.remove(streamId);
                }
            }
            session.close();
            log.info("销毁转发会话: streamId={}", streamId);
        }
        return session;
    }

    /**
     * 处理 Channel 断开
     * <p>
     * 当连接断开时，清理该连接关联的所有转发关系
     *
     * @param channel 断开的 Channel
     * @return 受影响的流 ID 列表
     */
    public List<String> handleChannelClosed(Channel channel) {
        Set<String> streamIds = channelSessions.remove(channel);
        if (streamIds == null || streamIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> affectedStreams = new ArrayList<>();
        for (String streamId : streamIds) {
            ForwardingSession session = forwardingSessions.get(streamId);
            if (session == null) continue;

            // 检查是否是推流者断开
            if (session.getPublisherChannel() != null && session.getPublisherChannel().equals(channel)) {
                // 推流者断开，销毁整个会话
                destroySession(streamId);
                affectedStreams.add(streamId);
            } else {
                // 订阅者断开，只移除该订阅者
                session.removeSubscriberByChannel(channel);
            }
        }

        return affectedStreams;
    }

    /**
     * 更新连接状态
     *
     * @param streamId    流 ID
     * @param userId      用户 ID
     * @param state       连接状态
     * @param isPublisher 是否是推流者
     */
    public void updateConnectionState(String streamId, String userId, String state, boolean isPublisher) {
        ForwardingSession session = forwardingSessions.get(streamId);
        if (session == null) return;

        if (isPublisher) {
            session.setPublisherState(state);
        } else {
            SubscriberInfo subscriber = session.getSubscriber(userId);
            if (subscriber != null) {
                subscriber.setConnectionState(state);
            }
        }

        log.debug("连接状态更新: streamId={}, userId={}, state={}, isPublisher={}",
                streamId, userId, state, isPublisher);
    }

    /**
     * 记录转发统计
     *
     * @param bytes    字节数
     * @param messages 消息数
     */
    public void recordForwarding(long bytes, long messages) {
        totalForwardedBytes.addAndGet(bytes);
        totalForwardedMessages.addAndGet(messages);
    }

    /**
     * 获取所有活跃的转发会话
     *
     * @return 会话列表
     */
    public List<ForwardingSession> getActiveSessions() {
        return new ArrayList<>(forwardingSessions.values());
    }

    /**
     * 获取转发会话数量
     *
     * @return 会话数量
     */
    public int getSessionCount() {
        return forwardingSessions.size();
    }

    /**
     * 获取总订阅者数量
     *
     * @return 订阅者数量
     */
    public int getTotalSubscriberCount() {
        return forwardingSessions.values().stream()
                .mapToInt(ForwardingSession::getSubscriberCount)
                .sum();
    }

    /**
     * 获取统计快照
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "activeSessions", getSessionCount(),
                "totalSubscribers", getTotalSubscriberCount(),
                "totalForwardedBytes", totalForwardedBytes.get(),
                "totalForwardedMessages", totalForwardedMessages.get()
        );
    }

    /**
     * 输出统计日志
     */
    private void logStats() {
        int sessions = getSessionCount();
        int subscribers = getTotalSubscriberCount();
        long bytes = totalForwardedBytes.get();
        long messages = totalForwardedMessages.get();

        if (sessions > 0 || subscribers > 0) {
            log.info("MediaForwarder 统计: 活跃会话={}, 订阅者={}, 转发消息={}, 转发字节={}",
                    sessions, subscribers, messages, formatBytes(bytes));
        }
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 销毁
     */
    @Override
    @PreDestroy
    public void destroy() {
        log.info("MediaForwarder 正在关闭...");

        if (statsScheduler != null && !statsScheduler.isShutdown()) {
            statsScheduler.shutdown();
            try {
                if (!statsScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    statsScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                statsScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭所有会话
        for (String streamId : new ArrayList<>(forwardingSessions.keySet())) {
            destroySession(streamId);
        }

        channelSessions.clear();
        log.info("MediaForwarder 已关闭");
    }

    public boolean hasSession(String streamId) {
        return forwardingSessions.containsKey(streamId);
    }
}

