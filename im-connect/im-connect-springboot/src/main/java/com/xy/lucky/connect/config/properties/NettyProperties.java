package com.xy.lucky.connect.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

/**
 * Netty 配置属性类
 * <p>
 * 对应 application.yml 中的 netty.config 配置
 * <pre>
 * netty:
 *   config:
 *     protocol: proto
 *     heartBeatTime: 30000
 *     bossThreadSize: 4
 *     workThreadSize: 16
 *     tcp:
 *       enable: false
 *       port:
 *         - 9000
 *     websocket:
 *       path: /im
 *       enable: true
 *       port:
 *         - 19000
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "netty.config")
public class NettyProperties {

    /**
     * 序列化协议：json 或 proto
     */
    private String protocol = "proto";

    /**
     * 心跳间隔时间（毫秒）
     */
    private int heartBeatTime = 30000;

    /**
     * 超时时间（毫秒）
     */
    private int timeout = 5000;

    /**
     * Boss 线程池大小
     */
    private int bossThreadSize = 4;

    /**
     * Worker 线程池大小
     */
    private int workThreadSize = 16;

    /**
     * 是否启用多设备登录
     */
    private Boolean multiDeviceEnabled = false;

    /**
     * TCP 配置
     */
    @NestedConfigurationProperty
    private TcpConfig tcp = new TcpConfig();

    /**
     * WebSocket 配置
     */
    @NestedConfigurationProperty
    private WebSocketConfig websocket = new WebSocketConfig();

    /**
     * TCP 配置
     */
    @Data
    public static class TcpConfig {
        /**
         * 是否启用 TCP
         */
        private boolean enable = false;

        /**
         * TCP 监听端口列表
         */
        private List<Integer> port;
    }

    /**
     * WebSocket 配置
     */
    @Data
    public static class WebSocketConfig {
        /**
         * WebSocket 路径
         */
        private String path = "/im";

        /**
         * 是否启用 WebSocket
         */
        private boolean enable = true;

        /**
         * WebSocket 监听端口列表
         */
        private List<Integer> port;
    }
}
