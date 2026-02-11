package com.xy.lucky.connect.netty.service;


import com.xy.lucky.connect.config.LogConstant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启停netty服务
 */
@Slf4j(topic = LogConstant.Netty)
public abstract class AbstractRemoteServer {

    // 启动标识（线程安全）
    protected final AtomicBoolean ready = new AtomicBoolean(false);
    // Netty bootstrap / groups（由子类继承或在此定义）
    protected ServerBootstrap bootstrap;
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
}
