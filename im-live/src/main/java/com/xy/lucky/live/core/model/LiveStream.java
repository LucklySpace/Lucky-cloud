package com.xy.lucky.live.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播流实体
 * <p>
 * 表示一个推流，包含流信息、发布者、订阅者等
 *
 * @author lucky
 */
@Data
@Builder
public class LiveStream {

    /**
     * 流 ID
     */
    private final String streamId;
    /**
     * 所属房间 ID
     */
    private final String roomId;
    /**
     * 发布者用户 ID
     */
    private final String publisherId;
    /**
     * 发布时间
     */
    @Builder.Default
    private final long publishTime = System.currentTimeMillis();
    /**
     * 订阅者用户 ID 集合
     */
    @Builder.Default
    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();
    /**
     * 扩展数据
     */
    @Builder.Default
    private final Map<String, Object> extra = new ConcurrentHashMap<>();
    /**
     * 流名称
     */
    private String name;
    /**
     * 流类型
     *
     * @see StreamType
     */
    @Builder.Default
    private StreamType type = StreamType.CAMERA;
    /**
     * 流状态
     */
    @Builder.Default
    private volatile StreamState state = StreamState.PUBLISHING;
    /**
     * 是否有音频
     */
    @Builder.Default
    private boolean hasAudio = true;
    /**
     * 是否有视频
     */
    @Builder.Default
    private boolean hasVideo = true;
    /**
     * 视频宽度
     */
    private int videoWidth;
    /**
     * 视频高度
     */
    private int videoHeight;
    /**
     * 帧率
     */
    private int frameRate;
    /**
     * 码率（kbps）
     */
    private int bitrate;

    /**
     * 添加订阅者
     *
     * @param userId 用户 ID
     * @return 是否添加成功
     */
    public boolean addSubscriber(String userId) {
        return subscribers.add(userId);
    }

    /**
     * 移除订阅者
     *
     * @param userId 用户 ID
     * @return 是否移除成功
     */
    public boolean removeSubscriber(String userId) {
        return subscribers.remove(userId);
    }

    /**
     * 检查是否是订阅者
     *
     * @param userId 用户 ID
     * @return 是否订阅
     */
    public boolean isSubscriber(String userId) {
        return subscribers.contains(userId);
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
     * 获取流信息快照
     *
     * @return 流信息 Map
     */
    public Map<String, Object> toSnapshot() {
        return Map.of(
                "streamId", streamId,
                "name", name != null ? name : streamId,
                "roomId", roomId,
                "publisherId", publisherId,
                "publishTime", publishTime,
                "type", type.name(),
                "state", state.name(),
                "hasAudio", hasAudio,
                "hasVideo", hasVideo,
                "subscriberCount", getSubscriberCount()
        );
    }

    /**
     * 流类型枚举
     */
    public enum StreamType {
        /**
         * 摄像头采集
         */
        CAMERA,

        /**
         * 屏幕共享
         */
        SCREEN,

        /**
         * 自定义媒体
         */
        CUSTOM
    }

    /**
     * 流状态枚举
     */
    public enum StreamState {
        /**
         * 正在发布
         */
        PUBLISHING,

        /**
         * 已暂停
         */
        PAUSED,

        /**
         * 已停止
         */
        STOPPED
    }
}

