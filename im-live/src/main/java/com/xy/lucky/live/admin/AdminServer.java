package com.xy.lucky.live.admin;

import com.xy.lucky.live.webrtc.RoomService;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 管理与健康检查服务（Netty HTTP）
 * - /health
 * - /metrics
 */
@Component
public class AdminServer implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(AdminServer.class);

    @Autowired
    private RoomService roomService;

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
                    .childHandler(new io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
                        @Override
                        protected void initChannel(io.netty.channel.socket.SocketChannel ch) {
                            var p = ch.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(32 * 1024));
                            p.addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
                                    String uri = req.uri();
                                    if (uri.startsWith("/health")) {
                                        writeJson(ctx, 200, "{\"status\":\"UP\"}");
                                    } else if (uri.startsWith("/metrics")) {
                                        Map<String, Object> m = Map.of(
                                                "rooms", roomServiceMetricRooms(),
                                                "publishers", roomServiceMetricPublishers(),
                                                "subscribers", roomServiceMetricSubscribers()
                                        );
                                        String json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(m).toString();
                                        writeJson(ctx, 200, json);
                                    } else {
                                        writeJson(ctx, 404, "{\"error\":\"Not Found\"}");
                                    }
                                }
                            });
                        }
                    });
            serverChannel = b.bind(port).sync().channel();
            log.info("Admin server bind on port {}", port);
        } catch (Exception e) {
            log.error("Admin 服务器启动失败", e);
            throw e;
        }
    }

    private void writeJson(ChannelHandlerContext ctx, int code, String body) {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(code), Unpooled.wrappedBuffer(data));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, data.length);
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }

    private int roomServiceMetricRooms() {
        try {
            var field = RoomService.class.getDeclaredField("rooms");
            field.setAccessible(true);
            Map<?, ?> rooms = (Map<?, ?>) field.get(roomService);
            return rooms.size();
        } catch (Exception e) {
            return -1;
        }
    }

    private int roomServiceMetricPublishers() {
        // 简化：无 RTMP registry 注入，这里返回 -1 或后续扩展
        return -1;
    }

    private int roomServiceMetricSubscribers() {
        return -1;
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
        log.info("Admin server stopped");
    }
}

