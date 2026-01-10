package com.xy.lucky.live.server;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.core.sfu.SfuService;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * WebRTC UDP 服务器
 * <p>
 * 基于 Netty 的 UDP 服务器，用于处理 WebRTC 媒体流量。
 * 遵循 SRS WebRTC 架构设计，使用单一端口复用多种协议。
 *
 * <h2>单端口复用架构</h2>
 * <p>
 * WebRTC 使用单一 UDP 端口处理所有类型的流量：
 * <ul>
 *   <li><b>STUN</b> (0-1): ICE 连接建立和地址发现</li>
 *   <li><b>DTLS</b> (20-63): DTLS 握手，用于 SRTP 密钥交换</li>
 *   <li><b>RTP</b> (128-191, PT=96-127): 音视频媒体数据</li>
 *   <li><b>RTCP</b> (128-191, PT=200-210): 控制协议，质量反馈</li>
 * </ul>
 *
 * <h2>数据流向</h2>
 * <pre>
 * 客户端 ──UDP──► WebRTCServer ──► WebRTCServerHandler ──► SfuService
 *                                                          │
 *                                                          ├─► STUN 处理
 *                                                          ├─► DTLS 处理
 *                                                          ├─► RTP 转发
 *                                                          └─► RTCP 处理
 * </pre>
 *
 * <h2>性能优化</h2>
 * <ul>
 *   <li>使用 Epoll (Linux) 或 NIO (其他平台) 实现高效 I/O</li>
 *   <li>大缓冲区设置（10MB）以应对高并发场景</li>
 *   <li>支持 SO_BROADCAST 用于多播场景</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 * @see WebRTCServerHandler
 * @see SfuService
 */
@Component
public class WebRTCServer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(WebRTCServer.class);

    /**
     * EventLoop 线程组，用于处理 UDP I/O
     */
    private EventLoopGroup group;

    /**
     * UDP Channel
     */
    private Channel channel;

    /**
     * 服务器是否已启动
     */
    private volatile boolean started = false;

    @Autowired
    private LiveProperties liveProperties;

    @Autowired
    private SfuService sfuService;

    /**
     * 初始化并启动服务器
     */
    @PostConstruct
    public void init() {
        start();
    }

    /**
     * 启动 WebRTC UDP 服务器
     */
    public void start() {
        if (started) {
            log.warn("WebRTC UDP 服务器已经启动");
            return;
        }

        LiveProperties.RtcConfig rtcConfig = liveProperties.getRtc();
        if (!rtcConfig.isEnabled()) {
            log.info("WebRTC UDP 服务器已禁用（配置中 rtc.enabled=false）");
            return;
        }

        // 根据配置选择 I/O 模型
        LiveProperties.PerformanceConfig perf = liveProperties.getPerformance();
        boolean useEpoll = perf.isPreferEpoll() && Epoll.isAvailable();
        int workerThreads = perf.getWorkerThreads() > 0
                ? perf.getWorkerThreads()
                : Runtime.getRuntime().availableProcessors() * 2;

        // 创建 EventLoopGroup
        if (useEpoll) {
            group = new EpollEventLoopGroup(workerThreads);
            log.info("使用 Epoll 事件循环（Linux 高性能 I/O）");
        } else {
            group = new NioEventLoopGroup(workerThreads);
            log.info("使用 NIO 事件循环");
        }

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(useEpoll ? EpollDatagramChannel.class : NioDatagramChannel.class)
                    // 启用广播，用于多播场景
                    .option(ChannelOption.SO_BROADCAST, true)
                    // 设置接收缓冲区大小（10MB），应对高并发
                    .option(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                    // 设置发送缓冲区大小（10MB）
                    .option(ChannelOption.SO_SNDBUF, 10 * 1024 * 1024)
                    // 设置重用地址
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // 设置 UDP 数据包处理器
                    .handler(new WebRTCServerHandler(sfuService));

            // 绑定端口
            ChannelFuture future = bootstrap.bind(rtcConfig.getHost(), rtcConfig.getPort()).sync();
            channel = future.channel();
            started = true;

            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║              WebRTC UDP 服务器启动成功                      ║");
            log.info("║  监听地址: UDP {}:{}", rtcConfig.getHost(), rtcConfig.getPort());
            log.info("║  IO 模型: {}", useEpoll ? "Epoll (Linux)" : "NIO");
            log.info("║  工作线程: {}", workerThreads);
            log.info("║  协议支持: STUN / DTLS / RTP / RTCP");
            log.info("╚══════════════════════════════════════════════════════════════╣");

        } catch (Exception e) {
            log.error("WebRTC UDP 服务器启动失败", e);
            shutdown();
            throw new RuntimeException("WebRTC UDP 服务器启动失败", e);
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (!started) {
            return;
        }

        if (channel != null && channel.isActive()) {
            channel.close().awaitUninterruptibly();
        }

        if (group != null && !group.isShutdown()) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly();
        }

        started = false;
        log.info("WebRTC UDP 服务器已关闭");
    }

    /**
     * 检查服务器是否运行中
     *
     * @return true 如果服务器正在运行
     */
    public boolean isRunning() {
        return started && channel != null && channel.isActive();
    }

    /**
     * 获取服务器绑定的端口
     *
     * @return 端口号，如果未启动返回 -1
     */
    public int getPort() {
        if (channel != null && channel.localAddress() != null) {
            return ((java.net.InetSocketAddress) channel.localAddress()).getPort();
        }
        return -1;
    }

    /**
     * 销毁时关闭服务器
     */
    @Override
    @PreDestroy
    public void destroy() {
        shutdown();
    }
}

