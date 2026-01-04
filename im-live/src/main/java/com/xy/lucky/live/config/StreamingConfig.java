package com.xy.lucky.live.config;

import com.xy.lucky.spring.annotations.core.Bean;
import com.xy.lucky.spring.annotations.core.Configuration;
import com.xy.lucky.spring.annotations.core.Value;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 流媒体服务配置
 * - 提供虚拟线程执行器（每任务一虚拟线程）
 * - 暴露端口配置注入字段（供 Runner/Server 使用）
 */
@Configuration
public class StreamingConfig {

    @Value("streaming.signaling.port")
    private int signalingPort = 8082;

    @Value("streaming.rtmp.port")
    private int rtmpPort = 1935;

    @Value("streaming.admin.port")
    private int adminPort = 8081;

    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    public int getSignalingPort() {
        return signalingPort;
    }

    public int getRtmpPort() {
        return rtmpPort;
    }

    public int getAdminPort() {
        return adminPort;
    }
}
