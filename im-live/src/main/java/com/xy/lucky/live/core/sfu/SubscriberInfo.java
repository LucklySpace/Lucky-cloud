package com.xy.lucky.live.core.sfu;

import io.netty.channel.Channel;
import lombok.Data;

import java.util.Map;

/**
 * 订阅者信息
 * <p>
 * 存储订阅者的连接信息和 WebRTC 会话状态。
 * 在 SFU 模式下，每个订阅者与推流者之间会建立独立的 PeerConnection。
 *
 * @author lucky
 * @version 1.0.0
 */
@Data
public class SubscriberInfo {

    /**
     * 订阅者用户 ID
     */
    private final String subscriberId;

    /**
     * 订阅者的 Channel
     */
    private final Channel channel;

    /**
     * 订阅的流 ID
     */
    private final String streamId;

    /**
     * 订阅时间
     */
    private final long subscribeTime;

    /**
     * WebRTC 连接状态
     * <ul>
     *   <li>new - 新建</li>
     *   <li>connecting - 连接中</li>
     *   <li>connected - 已连接</li>
     *   <li>disconnected - 已断开</li>
     *   <li>failed - 连接失败</li>
     *   <li>closed - 已关闭</li>
     * </ul>
     */
    private volatile String connectionState = "new";

    /**
     * ICE 连接状态
     */
    private volatile String iceConnectionState = "new";

    /**
     * ICE 收集状态
     */
    private volatile String iceGatheringState = "new";

    /**
     * 信令状态
     */
    private volatile String signalingState = "stable";

    /**
     * 是否已完成 SDP 交换
     */
    private volatile boolean sdpExchanged = false;

    /**
     * 收到的 ICE Candidate 数量
     */
    private volatile int candidateCount = 0;

    /**
     * 最后活跃时间
     */
    private volatile long lastActiveTime;

    /**
     * 构造函数
     *
     * @param subscriberId 订阅者 ID
     * @param channel      订阅者 Channel
     * @param streamId     流 ID
     */
    public SubscriberInfo(String subscriberId, Channel channel, String streamId) {
        this.subscriberId = subscriberId;
        this.channel = channel;
        this.streamId = streamId;
        this.subscribeTime = System.currentTimeMillis();
        this.lastActiveTime = this.subscribeTime;
    }

    /**
     * 更新活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 增加 Candidate 计数
     */
    public void incrementCandidateCount() {
        this.candidateCount++;
        updateActiveTime();
    }

    /**
     * 标记 SDP 已交换
     */
    public void markSdpExchanged() {
        this.sdpExchanged = true;
        updateActiveTime();
    }

    /**
     * 获取订阅时长（毫秒）
     *
     * @return 订阅时长
     */
    public long getDuration() {
        return System.currentTimeMillis() - subscribeTime;
    }

    /**
     * 检查连接是否健康
     *
     * @return 是否健康
     */
    public boolean isHealthy() {
        return "connected".equals(connectionState) || "connecting".equals(connectionState);
    }

    /**
     * 获取订阅者信息快照
     *
     * @return 订阅者信息 Map
     */
    public Map<String, Object> toSnapshot() {
        return Map.of(
                "subscriberId", subscriberId,
                "streamId", streamId,
                "connectionState", connectionState,
                "iceConnectionState", iceConnectionState,
                "iceGatheringState", iceGatheringState,
                "signalingState", signalingState,
                "sdpExchanged", sdpExchanged,
                "candidateCount", candidateCount,
                "duration", getDuration(),
                "subscribeTime", subscribeTime
        );
    }
}

