package com.xy.lucky.live.http;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.http.handler.RtcApiHandler;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * HTTP API 服务器
 * <p>
 * 提供 SRS 兼容的 WebRTC 推拉流 HTTP API 接口。
 * 参考 SRS 的架构设计，实现 `/rtc/v1/publish/` 和 `/rtc/v1/play/` 接口。
 *
 * <h2>支持的接口</h2>
 * <ul>
 *   <li><b>POST /rtc/v1/publish/</b>: WebRTC 推流接口</li>
 *   <li><b>POST /rtc/v1/play/</b>: WebRTC 拉流接口</li>
 * </ul>
 *
 * <h2>请求格式</h2>
 * <pre>
 * POST /rtc/v1/publish/?app=live&stream=stream1
 * Content-Type: application/json
 *
 * {
 *   "api": "webrtc://localhost/live/stream1",
 *   "sdp": "v=0\r\n..."
 * }
 * </pre>
 *
 * <h2>响应格式</h2>
 * <pre>
 * {
 *   "code": 0,
 *   "server": "im-live/1.0.0",
 *   "sdp": "v=0\r\n...",
 *   "sessionid": "xxx",
 *   "app": "live",
 *   "stream": "stream1"
 * }
 * </pre>
 *
 * @author lucky
 * @version 1.0.0
 * @see RtcApiHandler
 * @see <a href="https://github.com/ossrs/srs/wiki/v4_CN_WebRTC">SRS WebRTC Wiki</a>
 */
@Component
public class HttpApiServer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(HttpApiServer.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;
    private volatile boolean started = false;

    @Autowired
    private LiveProperties liveProperties;

    @Autowired
    private RtcApiService rtcApiService;

    @PostConstruct
    public void init() {
        start();
    }

    /**
     * 启动 HTTP API 服务器
     */
    public void start() {
        if (started) {
            log.warn("HTTP API 服务器已经启动");
            return;
        }

        LiveProperties.HttpApiConfig config = liveProperties.getHttpApi();
        if (!config.isEnabled()) {
            log.info("HTTP API 服务器已禁用（配置中 httpApi.enabled=false）");
            return;
        }

        LiveProperties.PerformanceConfig perf = liveProperties.getPerformance();
        boolean useEpoll = perf.isPreferEpoll() && Epoll.isAvailable();
        int workerThreads = perf.getWorkerThreads() > 0
                ? perf.getWorkerThreads()
                : Runtime.getRuntime().availableProcessors() * 2;

        // 创建 EventLoopGroup
        bossGroup = new NioEventLoopGroup(1);
        if (useEpoll) {
            workerGroup = new EpollEventLoopGroup(workerThreads);
        } else {
            workerGroup = new NioEventLoopGroup(workerThreads);
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, perf.getBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP 编解码器
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));

                            // 业务处理器（CORS 在处理器中手动实现）
                            pipeline.addLast(new RtcApiHandler(rtcApiService));
                        }
                    });

            ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
            channel = future.channel();
            started = true;

            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║            HTTP API 服务器启动成功 (SRS 兼容)              ║");
            log.info("║  监听地址: HTTP {}:{}", config.getHost(), config.getPort());
            log.info("║  API 路径: {}/publish/ 和 {}/play/", config.getApiBase(), config.getApiBase());
            log.info("║  IO 模型: {}", useEpoll ? "Epoll (Linux)" : "NIO");
            log.info("╚══════════════════════════════════════════════════════════════╣");

        } catch (Exception e) {
            log.error("HTTP API 服务器启动失败", e);
            shutdown();
            throw new RuntimeException("HTTP API 服务器启动失败", e);
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

        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly();
        }

        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly();
        }

        started = false;
        log.info("HTTP API 服务器已关闭");
    }

    /**
     * 检查服务器是否运行中
     *
     * @return true 如果服务器正在运行
     */
    public boolean isRunning() {
        return started && channel != null && channel.isActive();
    }

    @Override
    @PreDestroy
    public void destroy() {
        shutdown();
    }
}

