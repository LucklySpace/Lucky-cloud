package com.xy.lucky.live;

import com.xy.lucky.spring.boot.SpringApplication;
import com.xy.lucky.spring.boot.annotation.SpringBootApplication;

/**
 * im-live 直播服务应用启动类
 * <p>
 * 基于 im-spring 框架和 Netty 实现的高性能 WebRTC 推拉流直播服务
 *
 * <h2>功能特性</h2>
 * <ul>
 *   <li>WebRTC 信令交换（SDP/ICE 透传）</li>
 *   <li>多人直播房间管理</li>
 *   <li>推流/拉流/订阅管理</li>
 *   <li>虚拟线程支持高并发</li>
 * </ul>
 *
 * <h2>使用方式</h2>
 * <pre>
 * java -jar im-live.jar
 * </pre>
 *
 * @author lucky
 * @version 1.0.0
 */
@SpringBootApplication
public class ImLiveApplication {

    /**
     * 应用入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ImLiveApplication.class, args);
    }
}

