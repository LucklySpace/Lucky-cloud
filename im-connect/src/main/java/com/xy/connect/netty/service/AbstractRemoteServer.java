package com.xy.connect.netty.service;

import com.xy.connect.config.ConfigCenter;
import com.xy.connect.config.IMNacosConfig;
import com.xy.connect.config.IMNettyConfig;
import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.IMChannelHandler;
import com.xy.connect.netty.factory.NettyEventLoopFactory;
import com.xy.connect.netty.service.tcp.TcpSocketServer;
import com.xy.connect.netty.service.websocket.WebSocketServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j(topic = LogConstant.NETTY)
public class AbstractRemoteServer implements IMServer {

    public IMNettyConfig.NettyConfig nettyConfig;
    public IMNacosConfig.NacosConfig nacosConfig;

    protected ServerBootstrap bootstrap;
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workGroup;

    protected boolean ready = false;

    public AbstractRemoteServer() {

        nettyConfig = ConfigCenter.nettyConfig.getNettyConfig();

        nacosConfig = ConfigCenter.nacosConfig.getNacosConfig();

        bootstrap = new ServerBootstrap();

        bossGroup = NettyEventLoopFactory.eventLoopGroup(nettyConfig.getBossThreadSize());

        workGroup = NettyEventLoopFactory.eventLoopGroup(nettyConfig.getWorkThreadSize());
    }

    protected void addPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS));
        pipeline.addLast(new IMChannelHandler());
    }


    @Override
    public void start() {
        //启动TCP服务器
        if (ConfigCenter.nettyConfig.getNettyConfig().getTcpConfig().isEnable()) {
            new TcpSocketServer().start();
        }
        //启动WebSocket服务器
        if (ConfigCenter.nettyConfig.getNettyConfig().getWebSocketConfig().isEnable()) {
            new WebSocketServer().start();
        }
    }


    @Override
    public void stop() {
        if (bossGroup != null && !bossGroup.isShuttingDown() && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (workGroup != null && !workGroup.isShuttingDown() && !workGroup.isShutdown()) {
            workGroup.shutdownGracefully();
        }
        this.ready = false;
        log.error("server 停止");
    }

}
