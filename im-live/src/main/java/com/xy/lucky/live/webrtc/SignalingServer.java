package com.xy.lucky.live.webrtc;

import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebRTC 信令服务器（基于 Netty WebSocket）
 * - 只负责信令交换：加入/离开房间、SDP/ICE 透传
 * - 媒体传输由浏览器端 WebRTC 点对点完成
 */
@Component
public class SignalingServer implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SignalingServer.class);

    @Autowired
    private RoomService roomService;

    @Autowired
    private java.util.concurrent.ExecutorService virtualThreadExecutor;

    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private Channel serverChannel;

    public void start(int port) throws InterruptedException {
        boss = new NioEventLoopGroup(1);
        worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new SignalingInitializer(roomService, virtualThreadExecutor));

            serverChannel = b.bind(port).sync().channel();
            log.info("WebRTC Signaling server bind on port {}", port);
        } catch (Exception e) {
            log.error("WebRTC 信令服务器启动失败", e);
            throw e;
        }
    }

    @Override
    public void destroy() {
        try {
            if (serverChannel != null) {
                serverChannel.close().syncUninterruptibly();
            }
        } finally {
            if (worker != null) worker.shutdownGracefully();
            if (boss != null) boss.shutdownGracefully();
        }
        log.info("WebRTC Signaling server stopped");
    }
}
