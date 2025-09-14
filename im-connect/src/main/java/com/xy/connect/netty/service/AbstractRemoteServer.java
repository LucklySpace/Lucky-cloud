package com.xy.connect.netty.service;


import com.xy.connect.config.LogConstant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * 启停netty服务
 */
@Slf4j(topic = LogConstant.Netty)
public abstract class AbstractRemoteServer {

    protected ServerBootstrap bootstrap;

    protected EventLoopGroup bossGroup;

    protected EventLoopGroup workGroup;

    protected volatile boolean ready = false;

}
