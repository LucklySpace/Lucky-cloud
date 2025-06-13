package com.xy.connect.netty.service;


import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.IMChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 启停netty服务
 *
 * @author dense
 */
@Slf4j(topic = LogConstant.Netty)
public abstract class AbstractRemoteServer {

    protected ServerBootstrap bootstrap;
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workGroup;
    protected volatile boolean ready = false;


    protected void addPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS));
        pipeline.addLast(new IMChannelHandler());
    }


    /**
     * 优雅关闭Netty线程组
     */
    public void shutdown() {
        try {
            if (bossGroup != null) bossGroup.shutdownGracefully();
            if (workGroup != null) workGroup.shutdownGracefully();
            log.info("WebSocket 服务已优雅停止");
        } catch (Exception e) {
            log.error("WebSocket 优雅关闭失败", e);
        }
    }
}
