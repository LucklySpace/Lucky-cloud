package com.xy.lucky.live.core.sfu;

import com.xy.lucky.live.core.model.LiveStream;
import io.netty.channel.Channel;
import lombok.Data;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 媒体转发会话
 * <p>
 * 表示一个推流的转发会话，包含推流者信息和所有订阅者信息。
 * 在 SFU 架构中，每个推流对应一个转发会话。
 *
 * <h2>会话生命周期</h2>
 * <ol>
 *   <li>创建：推流者发布流时创建</li>
 *   <li>活跃：订阅者可以加入和离开</li>
 *   <li>销毁：推流者停止推流或断开连接时销毁</li>
 * </ol>
 *
 * @author lucky
 * @version 1.0.0
 */
@Data
public class ForwardingSession {

    /**
     * 所属房间 ID
     */
    private final String roomId;

    /**
     * 推流者用户 ID
     */
    private final String publisherId;

    /**
     * 流 ID
     */
    private final String streamId;

    /**
     * 关联的流信息
     */
    private final LiveStream stream;

    /**
     * 会话创建时间
     */
    private final long createTime;
    /**
     * 订阅者 ID -> 订阅者信息映射
     */
    private final Map<String, SubscriberInfo> subscribers = new ConcurrentHashMap<>();
    /**
     * 统计：已转发消息数
     */
    private final AtomicLong forwardedMessages = new AtomicLong(0);
    /**
     * 统计：已转发字节数
     */
    private final AtomicLong forwardedBytes = new AtomicLong(0);
    /**
     * 推流者的 Channel（用于快速关联）
     */
    private volatile Channel publisherChannel;
    /**
     * 推流者的连接状态
     */
    private volatile String publisherState = "new";
    /**
     * 会话是否已关闭
     */
    private volatile boolean closed = false;

    /**
     * 构造函数
     *
     * @param roomId      房间 ID
     * @param publisherId 推流者 ID
     * @param streamId    流 ID
     * @param stream      流信息
     */
    public ForwardingSession(String roomId, String publisherId, String streamId, LiveStream stream) {
        this.roomId = roomId;
        this.publisherId = publisherId;
        this.streamId = streamId;
        this.stream = stream;
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 添加订阅者
     *
     * @param subscriberId 订阅者 ID
     * @param channel      订阅者 Channel
     * @return 订阅者信息
     */
    public SubscriberInfo addSubscriber(String subscriberId, Channel channel) {
        if (closed) {
            return null;
        }

        SubscriberInfo info = new SubscriberInfo(subscriberId, channel, streamId);
        subscribers.put(subscriberId, info);
        return info;
    }

    /**
     * 移除订阅者
     *
     * @param subscriberId 订阅者 ID
     * @return 被移除的订阅者信息
     */
    public SubscriberInfo removeSubscriber(String subscriberId) {
        return subscribers.remove(subscriberId);
    }

    /**
     * 根据 Channel 移除订阅者
     *
     * @param channel Channel
     * @return 被移除的订阅者信息
     */
    public SubscriberInfo removeSubscriberByChannel(Channel channel) {
        for (Map.Entry<String, SubscriberInfo> entry : subscribers.entrySet()) {
            if (entry.getValue().getChannel().equals(channel)) {
                return subscribers.remove(entry.getKey());
            }
        }
        return null;
    }

    /**
     * 获取订阅者信息
     *
     * @param subscriberId 订阅者 ID
     * @return 订阅者信息
     */
    public SubscriberInfo getSubscriber(String subscriberId) {
        return subscribers.get(subscriberId);
    }

    /**
     * 检查是否有指定订阅者
     *
     * @param subscriberId 订阅者 ID
     * @return 是否存在
     */
    public boolean hasSubscriber(String subscriberId) {
        return subscribers.containsKey(subscriberId);
    }

    /**
     * 获取所有订阅者（只读视图）
     *
     * @return 订阅者映射
     */
    public Map<String, SubscriberInfo> getSubscribers() {
        return Collections.unmodifiableMap(subscribers);
    }

    /**
     * 获取订阅者数量
     *
     * @return 订阅者数量
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }

    /**
     * 记录转发统计
     *
     * @param bytes 字节数
     */
    public void recordForwarding(long bytes) {
        forwardedMessages.incrementAndGet();
        forwardedBytes.addAndGet(bytes);
    }

    /**
     * 获取会话持续时间（毫秒）
     *
     * @return 持续时间
     */
    public long getDuration() {
        return System.currentTimeMillis() - createTime;
    }

    /**
     * 获取会话信息快照
     *
     * @return 会话信息 Map
     */
    public Map<String, Object> toSnapshot() {
        return Map.of(
                "roomId", roomId,
                "publisherId", publisherId,
                "streamId", streamId,
                "publisherState", publisherState,
                "subscriberCount", getSubscriberCount(),
                "forwardedMessages", forwardedMessages.get(),
                "forwardedBytes", forwardedBytes.get(),
                "duration", getDuration(),
                "createTime", createTime,
                "closed", closed
        );
    }

    /**
     * 关闭会话
     */
    public void close() {
        closed = true;
        subscribers.clear();
    }
}

