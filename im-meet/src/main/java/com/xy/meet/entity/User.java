package com.xy.meet.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import io.netty.channel.Channel;
import lombok.*;

/**
 * 参加者实体（优化版）
 * <p>
 * - channel 为 transient 且 @JsonIgnore，避免序列化 Channel
 * - Role 枚举支持大小写兼容的反序列化
 * - 提供安全的更新与快照方法（toUserInfo）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "channel")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @EqualsAndHashCode.Include
    private String userId;

    /**
     * 保留 Channel 引用以便回写，但不应序列化/发送到客户端
     */
    @JsonIgnore
    private transient Channel channel;

    /**
     * 显示名
     */
    private String name;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 是否为本地用户（当前设备）
     */
    @Builder.Default
    private boolean isLocal = false;

    /**
     * 是否静音
     */
    @Builder.Default
    private boolean muted = false;

    /**
     * 摄像头开启
     */
    @Builder.Default
    private boolean cameraOn = false;

    /**
     * 是否共享屏幕
     */
    @Builder.Default
    private boolean screenShareOn = false;

    /**
     * 角色：HOST / PARTICIPANT（对外序列化为小写，接收大小写均可）
     */
    private Role role;

    /**
     * 当前连接状态
     */
    private ConnectionState connectionState;

    /**
     * 媒体流占位（可能为流 id / url 等）
     */
    private String stream;

    /**
     * 最近一次心跳时间戳（毫秒），用于超时检测
     * 使用 volatile 保证可见性，线程安全更新通过 refreshHeartbeat()
     */
    private volatile long lastHeartbeat;

    /* ---------------- 构造器便捷化 ---------------- */
    public User(String userId, Channel channel) {
        this.userId = userId;
        this.channel = channel;
        this.connectionState = ConnectionState.CONNECTED;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    /* ----------------- 枚举定义（带 JSON 兼容） ----------------- */

    /**
     * 刷新心跳时间并设置为 CONNECTED
     */
    public void refreshHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.connectionState = ConnectionState.CONNECTED;
    }

    /**
     * 标记断连（或进入重连状态）
     */
    public void markDisconnected() {
        this.connectionState = ConnectionState.DISCONNECTED;
    }

    /* ----------------- 常用方法 ----------------- */

    public void markReconnecting() {
        this.connectionState = ConnectionState.RECONNECTING;
    }

    /**
     * 安全更新：从另一个 User（通常来自解码或外部数据）更新非空字段
     * 不会覆盖 channel、userId（作为标识）；
     */
    public synchronized void updateFrom(User other) {
        if (other == null) return;
        // 保留 userId 不变
        if (other.getName() != null) this.name = other.getName();
        if (other.getAvatar() != null) this.avatar = other.getAvatar();
        this.isLocal = other.isLocal;
        this.muted = other.muted;
        this.cameraOn = other.cameraOn;
        this.screenShareOn = other.screenShareOn;
        if (other.getRole() != null) this.role = other.getRole();
        if (other.getConnectionState() != null) this.connectionState = other.getConnectionState();
        if (other.getStream() != null) this.stream = other.getStream();
        if (other.getLastHeartbeat() > 0) this.lastHeartbeat = other.getLastHeartbeat();
        // channel 不由此方法更新（应使用 updateChannel）
    }

    /**
     * 安全更新 Channel 引用（重连场景）
     */
    public synchronized void updateChannel(Channel newChannel) {
        if (newChannel == null) return;
        this.channel = newChannel;
        this.connectionState = ConnectionState.CONNECTED;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    /**
     * 快速判定是否在线
     */
    public boolean isOnline() {
        return this.connectionState == ConnectionState.CONNECTED;
    }

    public enum Role {
        HOST, PARTICIPANT;

        /**
         * Jackson 反序列化支持大小写及常见别名（如 "host","Host","HOST"）
         */
        @JsonCreator
        public static Role fromString(String key) {
            if (key == null) return null;
            switch (key.trim().toLowerCase()) {
                case "host":
                    return HOST;
                case "participant":
                    return PARTICIPANT;
                default:
                    return null; // 或者抛异常，根据业务决定
            }
        }

        /**
         * JSON 序列化输出（这里使用小写），便于前端统一显示/比较
         */
        @JsonValue
        public String toValue() {
            return this.name().toLowerCase();
        }
    }

    public enum ConnectionState {
        CONNECTED, RECONNECTING, DISCONNECTED;

        @JsonCreator
        public static ConnectionState fromString(String key) {
            if (key == null) return null;
            switch (key.trim().toLowerCase()) {
                case "connected":
                    return CONNECTED;
                case "reconnecting":
                    return RECONNECTING;
                case "disconnected":
                    return DISCONNECTED;
                default:
                    return null;
            }
        }

        @JsonValue
        public String toValue() {
            return this.name().toLowerCase();
        }
    }

}
