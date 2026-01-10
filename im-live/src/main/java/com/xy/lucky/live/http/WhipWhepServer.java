package com.xy.lucky.live.http;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * WHIP/WHEP HTTP 服务器
 * <p>
 * 基于 Netty 的 HTTP 服务器，提供 WHIP/WHEP 协议的 HTTP 接口。
 * 参考 SRS 的接口设计，支持基于 URL 的 WebRTC 推拉流。
 *
 * <h2>接口路径</h2>
 * <ul>
 *   <li>POST /rtc/v1/whip/?app={app}&stream={stream} - WHIP 推流</li>
 *   <li>DELETE /rtc/v1/whip/?app={app}&stream={stream} - 停止推流</li>
 *   <li>GET /rtc/v1/whep/?app={app}&stream={stream} - WHEP 拉流（获取 Offer）</li>
 *   <li>POST /rtc/v1/whep/?app={app}&stream={stream} - WHEP 拉流（发送 Answer）</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 */
@Component
public class WhipWhepServer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(WhipWhepServer.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean started = false;

    @Autowired
    private LiveProperties liveProperties;

    @Autowired
    private WhipWhepService whipWhepService;

    @PostConstruct
    public void init() {
        start();
    }

    /**
     * 启动 HTTP 服务器
     */
    public void start() {
        if (started) {
            return;
        }

        LiveProperties.PerformanceConfig perf = liveProperties.getPerformance();
        boolean useEpoll = perf.isPreferEpoll() && Epoll.isAvailable();
        int workerThreads = perf.getWorkerThreads() > 0
                ? perf.getWorkerThreads()
                : Runtime.getRuntime().availableProcessors() * 2;

        if (useEpoll) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup(workerThreads);
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(workerThreads);
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WhipWhepHandler(whipWhepService));
                        }
                    });

            // 使用信令服务器的端口 + 1，或者配置单独的端口
            int httpPort = liveProperties.getSignaling().getPort() + 1;
            ChannelFuture future = bootstrap.bind(httpPort).sync();
            serverChannel = future.channel();
            started = true;

            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║              WHIP/WHEP HTTP 服务器启动成功                  ║");
            log.info("║  监听地址: http://0.0.0.0:{}", httpPort);
            log.info("║  WHIP 推流: POST /rtc/v1/whip/?app={app}&stream={stream}");
            log.info("║  WHEP 拉流: GET /rtc/v1/whep/?app={app}&stream={stream}");
            log.info("╚══════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("WHIP/WHEP HTTP 服务器启动失败", e);
            shutdown();
            throw new RuntimeException("WHIP/WHEP HTTP 服务器启动失败", e);
        }
    }

    public void shutdown() {
        if (!started) {
            return;
        }

        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).awaitUninterruptibly();
        }

        started = false;
        log.info("WHIP/WHEP HTTP 服务器已关闭");
    }

    @Override
    @PreDestroy
    public void destroy() {
        shutdown();
    }

    /**
     * WHIP/WHEP HTTP 请求处理器
     */
    private static class WhipWhepHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final WhipWhepService service;

        public WhipWhepHandler(WhipWhepService service) {
            this.service = service;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            try {
                String uri = request.uri();
                HttpMethod method = request.method();

                // 解析查询参数
                QueryStringDecoder decoder = new QueryStringDecoder(uri);
                String app = decoder.parameters().getOrDefault("app", Collections.emptyList())
                        .stream().findFirst().orElse("live");
                String stream = decoder.parameters().getOrDefault("stream", Collections.emptyList())
                        .stream().findFirst().orElse(null);

                FullHttpResponse response;

                // WHIP 推流
                if (uri.startsWith("/rtc/v1/whip")) {
                    if (stream == null) {
                        response = createErrorResponse(HttpResponseStatus.BAD_REQUEST, "缺少 stream 参数");
                    } else {
                        String offerSdp = request.content().toString(CharsetUtil.UTF_8);
                        try {
                            String answerSdp = service.handleWhipPublish(app, stream, offerSdp);
                            response = createSdpResponse(HttpResponseStatus.CREATED, answerSdp);
                            response.headers().set("Location", "/rtc/v1/whip/?app=" + app + "&stream=" + stream);
                        } catch (Exception e) {
                            log.error("WHIP 推流失败", e);
                            response = createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                        }
                    }
                }
                // WHIP 停止推流
                else if (uri.startsWith("/rtc/v1/whip") && method == HttpMethod.DELETE) {
                    if (stream == null) {
                        response = createErrorResponse(HttpResponseStatus.BAD_REQUEST, "缺少 stream 参数");
                    } else {
                        try {
                            service.handleWhipUnpublish(app, stream);
                            response = createTextResponse(HttpResponseStatus.NO_CONTENT, "");
                        } catch (Exception e) {
                            log.error("停止 WHIP 推流失败", e);
                            response = createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                        }
                    }
                }
                // WHEP 拉流（获取 Offer）
                else if (uri.startsWith("/rtc/v1/whep") && method == HttpMethod.GET) {
                    if (stream == null) {
                        response = createErrorResponse(HttpResponseStatus.BAD_REQUEST, "缺少 stream 参数");
                    } else {
                        try {
                            String offerSdp = service.handleWhepPlay(app, stream);
                            response = createSdpResponse(HttpResponseStatus.OK, offerSdp);
                        } catch (Exception e) {
                            log.error("WHEP 拉流失败", e);
                            response = createErrorResponse(HttpResponseStatus.NOT_FOUND, e.getMessage());
                        }
                    }
                }
                // WHEP 拉流（发送 Answer）
                else if (uri.startsWith("/rtc/v1/whep") && method == HttpMethod.POST) {
                    if (stream == null) {
                        response = createErrorResponse(HttpResponseStatus.BAD_REQUEST, "缺少 stream 参数");
                    } else {
                        String answerSdp = request.content().toString(CharsetUtil.UTF_8);
                        try {
                            service.handleWhepAnswer(app, stream, answerSdp);
                            response = createTextResponse(HttpResponseStatus.CREATED, "");
                        } catch (Exception e) {
                            log.error("WHEP Answer 处理失败", e);
                            response = createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                        }
                    }
                }
                // 404
                else {
                    response = createErrorResponse(HttpResponseStatus.NOT_FOUND, "接口不存在");
                }

                // 设置 CORS 头
                response.headers().set("Access-Control-Allow-Origin", "*");
                response.headers().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
                response.headers().set("Access-Control-Allow-Headers", "Content-Type");

                ctx.writeAndFlush(response);

            } catch (Exception e) {
                log.error("处理 HTTP 请求异常", e);
                FullHttpResponse errorResponse = createErrorResponse(
                        HttpResponseStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
                ctx.writeAndFlush(errorResponse);
            }
        }

        /**
         * 创建 SDP 响应
         */
        private FullHttpResponse createSdpResponse(HttpResponseStatus status, String sdp) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status,
                    Unpooled.copiedBuffer(sdp, CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/sdp");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            return response;
        }

        /**
         * 创建文本响应
         */
        private FullHttpResponse createTextResponse(HttpResponseStatus status, String text) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status,
                    Unpooled.copiedBuffer(text, CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            return response;
        }

        /**
         * 创建错误响应
         */
        private FullHttpResponse createErrorResponse(HttpResponseStatus status, String message) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status,
                    Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            return response;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("HTTP 处理器异常", cause);
            ctx.close();
        }
    }
}

