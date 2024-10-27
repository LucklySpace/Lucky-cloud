package com.xy.connect.config;


import java.util.List;

/**
 * netty配置
 */
public class IMNettyConfig {

    private NettyConfig nettyConfig;

    public NettyConfig getNettyConfig() {
        return nettyConfig;
    }

    public void setNettyConfig(NettyConfig nettyConfig) {
        this.nettyConfig = nettyConfig;
    }

    @Override
    public String toString() {
        return "IMNettyConfig{" +
                "nettyConfig=" + nettyConfig +
                '}';
    }

    public static class NettyConfig {

        // 心跳超时时间  单位ms
        private Long heartBeatTime;

        // boss线程 默认=1
        private Integer bossThreadSize;

        //work线程
        private Integer workThreadSize;

        private TCPConfig tcpConfig;

        private WebSocketConfig webSocketConfig;


        public Long getHeartBeatTime() {
            return heartBeatTime;
        }

        public void setHeartBeatTime(Long heartBeatTime) {
            this.heartBeatTime = heartBeatTime;
        }

        public Integer getBossThreadSize() {
            return bossThreadSize;
        }

        public void setBossThreadSize(Integer bossThreadSize) {
            this.bossThreadSize = bossThreadSize;
        }

        public Integer getWorkThreadSize() {
            return workThreadSize;
        }

        public void setWorkThreadSize(Integer workThreadSize) {
            this.workThreadSize = workThreadSize;
        }

        public TCPConfig getTcpConfig() {
            return tcpConfig;
        }

        public void setTcpConfig(TCPConfig tcpConfig) {
            this.tcpConfig = tcpConfig;
        }

        public WebSocketConfig getWebSocketConfig() {
            return webSocketConfig;
        }

        public void setWebSocketConfig(WebSocketConfig webSocketConfig) {
            this.webSocketConfig = webSocketConfig;
        }

        @Override
        public String toString() {
            return "NettyConfig{" +
                    "heartBeatTime=" + heartBeatTime +
                    ", bossThreadSize=" + bossThreadSize +
                    ", workThreadSize=" + workThreadSize +
                    ", tcpConfig=" + tcpConfig +
                    ", webSocketConfig=" + webSocketConfig +
                    '}';
        }
    }

    public static class TCPConfig {
        private List<Integer> port;

        private boolean enable;

        public List<Integer> getPort() {
            return port;
        }

        public void setPort(List<Integer> port) {
            this.port = port;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        @Override
        public String toString() {
            return "TCPConfig{" +
                    "port=" + port +
                    ", enable=" + enable +
                    '}';
        }
    }

    public static class WebSocketConfig {

        private List<Integer> port;

        private boolean enable;

        public List<Integer> getPort() {
            return port;
        }

        public void setPort(List<Integer> port) {
            this.port = port;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        @Override
        public String toString() {
            return "WebSocketConfig{" +
                    "port=" + port +
                    ", enable=" + enable +
                    '}';
        }
    }
}
