package com.xy.lucky.live.core.model;

import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播用户实体
 * <p>
 * 表示一个加入直播房间的用户，包含连接信息、推流列表、订阅列表等
 *
 * @author lucky
 */
@Data
@Builder
public class LiveUser {

    /**
     * 用户 ID
     */
    private final String userId;
    /**
     * 用户所在房间 ID
     */
    private final String roomId;
    /**
     * 用户的 WebSocket 连接
     */
    private final Channel channel;
    /**
     * 加入时间
     */
    @Builder.Default
    private final long joinTime = System.currentTimeMillis();
    /**
     * 用户发布的流 ID 集合
     */
    @Builder.Default
    private final Set<String> publishedStreams = ConcurrentHashMap.newKeySet();
    /**
     * 用户订阅的流 ID 集合
     */
    @Builder.Default
    private final Set<String> subscribedStreams = ConcurrentHashMap.newKeySet();
    /**
     * 用户媒体状态
     */
    @Builder.Default
    private final MediaState mediaState = new MediaState();
    /**
     * 用户扩展数据
     */
    @Builder.Default
    private final Map<String, Object> extra = new ConcurrentHashMap<>();
    /**
     * 用户名称
     */
    private String name;
    /**
     * 用户角色
     *
     * @see UserRole
     */
    @Builder.Default
    private UserRole role = UserRole.VIEWER;

    /**
     * 添加发布的流
     *
     * @param streamId 流 ID
     */
    public void addPublishedStream(String streamId) {
        publishedStreams.add(streamId);
        // 有推流的用户自动升级为主播
        if (role == UserRole.VIEWER) {
            role = UserRole.BROADCASTER;
        }
    }

    /**
     * 移除发布的流
     *
     * @param streamId 流 ID
     */
    public void removePublishedStream(String streamId) {
        publishedStreams.remove(streamId);
        // 没有推流的用户降级为观众
        if (publishedStreams.isEmpty() && role == UserRole.BROADCASTER) {
            role = UserRole.VIEWER;
        }
    }

    /**
     * 添加订阅的流
     *
     * @param streamId 流 ID
     */
    public void addSubscribedStream(String streamId) {
        subscribedStreams.add(streamId);
    }

    /**
     * 移除订阅的流
     *
     * @param streamId 流 ID
     */
    public void removeSubscribedStream(String streamId) {
        subscribedStreams.remove(streamId);
    }

    /**
     * 是否是推流者
     *
     * @return 是否推流
     */
    public boolean isPublishing() {
        return !publishedStreams.isEmpty();
    }

    /**
     * 获取用户信息快照
     *
     * @return 用户信息 Map
     */
    public Map<String, Object> toSnapshot() {
        return Map.of(
                "userId", userId,
                "name", name != null ? name : userId,
                "role", role.name(),
                "joinTime", joinTime,
                "publishedStreams", publishedStreams,
                "subscribedStreams", subscribedStreams,
                "mediaState", mediaState.toMap()
        );
    }

    /**
     * 用户角色枚举
     */
    public enum UserRole {
        /**
         * 观众
         */
        VIEWER,

        /**
         * 主播（推流者）
         */
        BROADCASTER,

        /**
         * 管理员
         */
        ADMIN
    }

    /**
     * 媒体状态
     */
    @Data
    public static class MediaState {
        /**
         * 音频是否静音
         */
        private volatile boolean audioMuted = false;

        /**
         * 视频是否关闭
         */
        private volatile boolean videoMuted = false;

        /**
         * 转换为 Map
         */
        public Map<String, Object> toMap() {
            return Map.of(
                    "audioMuted", audioMuted,
                    "videoMuted", videoMuted
            );
        }
    }
}

