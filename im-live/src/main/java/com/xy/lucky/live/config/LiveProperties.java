package com.xy.lucky.live.config;

import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.boot.annotation.ConfigurationProperties;
import com.xy.lucky.spring.boot.annotation.NestedConfigurationProperty;
import lombok.Data;

import java.util.List;

/**
 * 直播服务配置属性类
 * <p>
 * 从 application.yml 中读取 live 前缀的配置
 * 支持 WebRTC 信令服务器、ICE 服务器等配置
 *
 * @author lucky
 */
@Data
@Component
@ConfigurationProperties(prefix = "live")
public class LiveProperties {

    /**
     * 信令服务器配置
     */
    @NestedConfigurationProperty
    private SignalingConfig signaling = new SignalingConfig();

    /**
     * ICE 服务器配置
     */
    @NestedConfigurationProperty
    private IceConfig ice = new IceConfig();

    /**
     * 房间配置
     */
    @NestedConfigurationProperty
    private RoomConfig room = new RoomConfig();

    /**
     * 性能配置
     */
    @NestedConfigurationProperty
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * WebRTC RTC 服务器配置
     */
    @NestedConfigurationProperty
    private RtcConfig rtc = new RtcConfig();

    /**
     * HTTP API 服务器配置（SRS 兼容接口）
     */
    @NestedConfigurationProperty
    private HttpApiConfig httpApi = new HttpApiConfig();

    /**
     * 信令服务器配置
     */
    @Data
    public static class SignalingConfig {
        /**
         * 信令服务器端口
         */
        private int port = 8082;

        /**
         * WebSocket 路径
         */
        private String path = "/ws";

        /**
         * 是否启用 SSL
         */
        private boolean ssl = false;

        /**
         * 心跳间隔（毫秒）
         */
        private long heartbeatInterval = 30000;

        /**
         * 心跳超时时间（毫秒）
         */
        private long heartbeatTimeout = 90000;

        /**
         * 最大帧大小（字节）
         */
        private int maxFrameSize = 65536;
    }

    /**
     * ICE 服务器配置
     */
    @Data
    public static class IceConfig {
        /**
         * STUN 服务器列表
         */
        private List<String> stunServers = List.of(
                "stun:stun.l.google.com:19302",
                "stun:stun1.l.google.com:19302"
        );

        /**
         * TURN 服务器列表
         */
        private List<TurnServer> turnServers = List.of();

        /**
         * ICE 收集超时时间（毫秒）
         */
        private long gatheringTimeout = 10000;
    }

    /**
     * TURN 服务器配置
     */
    @Data
    public static class TurnServer {
        /**
         * TURN 服务器 URL
         */
        private String url;

        /**
         * 用户名
         */
        private String username;

        /**
         * 凭证/密码
         */
        private String credential;
    }

    /**
     * 房间配置
     */
    @Data
    public static class RoomConfig {
        /**
         * 单个房间最大用户数
         */
        private int maxUsersPerRoom = 100;

        /**
         * 单个房间最大推流数
         */
        private int maxPublishersPerRoom = 10;

        /**
         * 房间空闲超时时间（毫秒），超时后自动销毁
         */
        private long idleTimeout = 300000;

        /**
         * 是否允许匿名加入
         */
        private boolean allowAnonymous = true;
    }

    /**
     * 性能配置
     */
    @Data
    public static class PerformanceConfig {
        /**
         * Boss 线程数
         */
        private int bossThreads = 1;

        /**
         * Worker 线程数
         */
        private int workerThreads = 0; // 0 表示使用默认值

        /**
         * 是否使用虚拟线程处理业务逻辑
         */
        private boolean useVirtualThreads = true;

        /**
         * 虚拟线程池名称前缀
         */
        private String virtualThreadNamePrefix = "live-vt-";

        /**
         * 连接队列大小
         */
        private int backlog = 1024;

        /**
         * 是否启用 Epoll（Linux）
         */
        private boolean preferEpoll = true;
    }

    /**
     * WebRTC RTC 服务器配置
     */
    @Data
    public static class RtcConfig {
        /**
         * 是否启用 WebRTC UDP 服务器
         */
        private boolean enabled = true;

        /**
         * 服务器监听地址（IP 或 0.0.0.0）
         */
        private String host = "0.0.0.0";

        /**
         * 服务器监听端口
         * <p>
         * 注意：此端口用于处理所有 WebRTC 流量（STUN/DTLS/RTP/RTCP）
         */
        private int port = 8000;

        /**
         * 服务器候选 IP 地址
         * <p>
         * 用于生成 SDP Answer 中的候选地址。
         * 必须配置为客户端可以访问的 IP 地址（不能是 127.0.0.1 或 0.0.0.0）。
         * 如果服务器在 NAT 后，应配置为公网 IP 或内网 IP。
         */
        private String candidateIp = "127.0.0.1";

        /**
         * 服务器候选端口
         * <p>
         * 通常与 port 相同，但在某些场景下可能不同。
         */
        private int candidatePort = 8000;
    }

    /**
     * HTTP API 服务器配置
     * <p>
     * 提供 SRS 兼容的 WebRTC 推拉流接口
     */
    @Data
    public static class HttpApiConfig {
        /**
         * 是否启用 HTTP API 服务器
         */
        private boolean enabled = true;

        /**
         * 服务器监听地址（IP 或 0.0.0.0）
         */
        private String host = "0.0.0.0";

        /**
         * 服务器监听端口（SRS 默认 1985）
         */
        private int port = 1985;

        /**
         * API 基础路径
         */
        private String apiBase = "/rtc/v1";
    }
}

