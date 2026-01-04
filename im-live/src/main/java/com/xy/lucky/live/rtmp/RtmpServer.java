package com.xy.lucky.live.rtmp;

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
 * RTMP 服务端
 * - 支持基础握手与命令处理（connect/createStream/publish/play）
 * - 媒体数据转发留作后续扩展（可落地为 ChannelGroup 中继）
 */
@Component
public class RtmpServer implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(RtmpServer.class);

    @Autowired
    private StreamRegistry streamRegistry;

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
                    .childHandler(new RtmpInitializer(streamRegistry));
            serverChannel = b.bind(port).sync().channel();
            log.info("RTMP server bind on port {}", port);
        } catch (Exception e) {
            log.error("RTMP 服务器启动失败", e);
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
        log.info("RTMP server stopped");
    }
}

