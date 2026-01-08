package com.xy.lucky.live.server;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.signal.SignalDispatcher;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * WebRTC 信令服务器
 * <p>
 * 基于 Netty 实现的高性能 WebSocket 服务器
 * 支持 Epoll（Linux）和 NIO 两种模式
 * 使用虚拟线程处理业务逻辑，实现高并发低延迟
 *
 * @author lucky
 */
@Component
public class SignalingServer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SignalingServer.class);

    /**
     * Boss 线程组，负责接收连接
     */
    private EventLoopGroup bossGroup;

    /**
     * Worker 线程组，负责处理 I/O
     */
    private EventLoopGroup workerGroup;

    /**
     * 服务器 Channel
     */
    private Channel serverChannel;

    /**
     * 服务器是否已启动
     */
    private volatile boolean started = false;

    @Autowired
    private LiveProperties liveProperties;

    @Autowired
    private SignalDispatcher signalDispatcher;

    /**
     * 初始化并启动服务器
     */
    @PostConstruct
    public void init() {
        start();
    }

    /**
     * 启动信令服务器
     */
    public void start() {
        if (started) {
            log.warn("信令服务器已经在运行中");
            return;
        }

        LiveProperties.PerformanceConfig perf = liveProperties.getPerformance();
        LiveProperties.SignalingConfig signaling = liveProperties.getSignaling();

        // 判断是否使用 Epoll
        boolean useEpoll = perf.isPreferEpoll() && Epoll.isAvailable();

        // 计算线程数
        int bossThreads = perf.getBossThreads() > 0 ? perf.getBossThreads() : 1;
        int workerThreads = perf.getWorkerThreads() > 0 ?
                perf.getWorkerThreads() : Runtime.getRuntime().availableProcessors() * 2;

        // 创建线程组
        if (useEpoll) {
            log.info("使用 Epoll 模式");
            bossGroup = new EpollEventLoopGroup(bossThreads);
            workerGroup = new EpollEventLoopGroup(workerThreads);
        } else {
            log.info("使用 NIO 模式");
            bossGroup = new NioEventLoopGroup(bossThreads);
            workerGroup = new NioEventLoopGroup(workerThreads);
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    // 连接队列大小
                    .option(ChannelOption.SO_BACKLOG, perf.getBacklog())
                    // 地址复用
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // 使用池化的 ByteBuf 分配器
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    // 子 Channel 配置
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    // Channel 初始化器
                    .childHandler(new WebSocketInitializer(liveProperties, signalDispatcher));

            // 绑定端口并启动
            ChannelFuture future = bootstrap.bind(signaling.getPort()).sync();
            serverChannel = future.channel();
            started = true;

            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║                   WebRTC 信令服务器已启动                      ║");
            log.info("╠══════════════════════════════════════════════════════════════╣");
            log.info("║  WebSocket 地址: ws://localhost:{}{}", signaling.getPort(), signaling.getPath());
            log.info("║  Boss 线程数: {}", bossThreads);
            log.info("║  Worker 线程数: {}", workerThreads);
            log.info("║  使用虚拟线程: {}", perf.isUseVirtualThreads());
            log.info("║  传输模式: {}", useEpoll ? "Epoll" : "NIO");
            log.info("╚══════════════════════════════════════════════════════════════╝");

        } catch (InterruptedException e) {
            log.error("信令服务器启动被中断", e);
            Thread.currentThread().interrupt();
            shutdown();
        } catch (Exception e) {
            log.error("信令服务器启动失败", e);
            shutdown();
            throw new RuntimeException("信令服务器启动失败", e);
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        log.info("正在关闭信令服务器...");

        // 关闭服务器 Channel
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 优雅关闭线程组
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }

        started = false;
        log.info("信令服务器已关闭");
    }

    /**
     * 检查服务器是否运行中
     */
    public boolean isRunning() {
        return started && serverChannel != null && serverChannel.isActive();
    }

    /**
     * 获取服务器端口
     */
    public int getPort() {
        return liveProperties.getSignaling().getPort();
    }

    /**
     * 实现 DisposableBean，在容器关闭时自动调用
     */
    @Override
    @PreDestroy
    public void destroy() {
        shutdown();
    }
}

