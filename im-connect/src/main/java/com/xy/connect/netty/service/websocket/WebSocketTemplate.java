package com.xy.connect.netty.service.websocket;

import cn.hutool.core.collection.CollectionUtil;
import com.xy.connect.config.LogConstant;
import com.xy.connect.nacos.NacosTemplate;
import com.xy.connect.netty.AuthHandler;
import com.xy.connect.netty.factory.NettyEventLoopFactory;
import com.xy.connect.netty.service.AbstractRemoteServer;
import com.xy.connect.netty.service.websocket.codec.json.JsonMessageDecoder;
import com.xy.connect.netty.service.websocket.codec.json.JsonMessageEncoder;
import com.xy.connect.netty.service.websocket.codec.proto.ProtobufMessageDecoder;
import com.xy.connect.netty.service.websocket.codec.proto.ProtobufMessageEncoder;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.PostConstruct;
import com.xy.spring.annotations.core.Value;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.IntStream;


/**
 * WS服务器
 */
@Slf4j(topic = LogConstant.Netty)
@Component
public class WebSocketTemplate extends AbstractRemoteServer {

    @Value("netty.config.websocket.enable")
    protected Boolean websocketEnable;

    @Value("netty.config.websocket.port")
    protected List<Integer> webSocketPort;

    @Value("netty.config.bossThreadSize")
    protected Integer bossThreadSize;

    @Value("netty.config.workThreadSize")
    protected Integer workThreadSize;
    // 协议类型配置: json 或 proto
    @Value("netty.config.protocol")
    protected String protocolType = "proto";
    @Value("brokerId")
    private String brokerId;
    @Autowired
    private AuthHandler authHandler;

    @Autowired
    private NacosTemplate nacosTemplate;

    private ChannelFuture[] channelFutures = null;


    @PostConstruct
    public void run() {
        try {
            if (!ready) {
                log.info("netty 队列 正在启动...");
                start();
            } else {
                log.debug("netty 队列 正在运行");
            }
            // JVM关闭时优雅退出
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } catch (Exception e) {
            log.error("RabbitMQ 运行异常", e);
            //Thread.currentThread().interrupt();
        }
    }

    public void start() {

        if (!websocketEnable) {
            log.info("WebSocket 服务器未启用");
            return;
        }

        bootstrap = new ServerBootstrap();

        bossGroup = NettyEventLoopFactory.eventLoopGroup(bossThreadSize);

        workGroup = NettyEventLoopFactory.eventLoopGroup(workThreadSize);

        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        // 设置为主从线程模型（boss负责接受连接，worker负责处理IO）
        bootstrap.group(bossGroup, workGroup)
                // 设置服务端NIO通信类型
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                // 设置TCP的参数，SO_BACKLOG表示队列大小，用于处理临时的高并发连接请求，合理设置能避免拒绝服务
                .option(ChannelOption.SO_BACKLOG, 1024)
                // 是否允许重用Socket地址，避免某些情况下的端口占用问题
                .option(ChannelOption.SO_REUSEADDR, true)
                // 接收缓冲区大小，根据需要调整，以减少大流量情况下数据包丢失的风险
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                // 是否开启 TCP 底层心跳机制 保持长连接状态，避免连接频繁断开重连
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。 禁用Nagle算法，减少延迟，提高实时性
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 设置ChannelPipeline，也就是业务职责链，由处理的Handler串联而成，由worker线程池处理
                .childHandler(new ChannelInitializer<>() {

                    // 初始化每个Channel的职责链,添加处理的Handler，通常包括消息编解码、业务处理，也可以是日志、权限、过滤等
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加HTTP编解码器
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        // 聚合HTTP消息，避免处理分段数据,最大64KB
                        pipeline.addLast("aggregator", new HttpObjectAggregator(1024 * 64));
                        // 支持大数据流的处理
                        pipeline.addLast("http-chunked", new ChunkedWriteHandler());

                        // 处理Http请求转WebSocket握手请求
                        pipeline.addLast(authHandler);
                        // 处理WebSocket握手和数据帧的协议管理
                        pipeline.addLast(new WebSocketServerProtocolHandler("/im"));

                        if (protocolType.equals("json")) {
                            // json 编码器，将消息对象转为WebSocket数据帧
                            pipeline.addLast("encode", new JsonMessageEncoder());
                            // json 解码器，将WebSocket数据帧转为消息对象
                            pipeline.addLast("decode", new JsonMessageDecoder());
                        } else if (protocolType.equals("proto")) {
                            // 编码器，将消息对象转为WebSocket数据帧
                            pipeline.addLast("encode", new ProtobufMessageEncoder());
                            // 解码器，将WebSocket数据帧转为消息对象
                            pipeline.addLast("decode", new ProtobufMessageDecoder());
                        } else {
                            throw new IllegalArgumentException("不支持的协议类型: " + protocolType);
                        }
                    }
                });
        // 绑定端口
        bindPort();
    }

    /**
     * 绑定端口
     */
    private void bindPort() {

        if (CollectionUtil.isEmpty(webSocketPort)) {
            log.warn("未配置任何 WebSocket 端口，启动终止");
            return;
        }

        this.channelFutures = new ChannelFuture[webSocketPort.size()];

        IntStream.range(0, webSocketPort.size()).parallel().forEach(i -> {

            Integer port = webSocketPort.get(i);

            try {

                ChannelFuture future = bootstrap.bind(port).sync();

                channelFutures[i] = future;

                nacosTemplate.registerNacos(port);

                log.info("WebSocket 端口绑定成功: {}", port);

                future.channel().closeFuture().addListener(cf -> {
                    log.warn("WebSocket 端口 [{}] 的 Channel 已关闭", port);
                    channelFutures[i] = null;
                });

            } catch (Exception e) {
                log.error("WebSocket 端口绑定失败: {}", port, e);
            }
        });
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
