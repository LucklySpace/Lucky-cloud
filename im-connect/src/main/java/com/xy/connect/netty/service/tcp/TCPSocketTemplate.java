package com.xy.connect.netty.service.tcp;

import cn.hutool.core.collection.CollectionUtil;
import com.xy.connect.config.LogConstant;
import com.xy.connect.nacos.NacosTemplate;
import com.xy.connect.netty.AuthHandler;
import com.xy.connect.netty.factory.NettyEventLoopFactory;
import com.xy.connect.netty.service.AbstractRemoteServer;
import com.xy.connect.netty.service.tcp.codec.json.JsonMessageDecoder;
import com.xy.connect.netty.service.tcp.codec.proto.ProtobufMessageEncoder;
import com.xy.connect.netty.service.websocket.codec.proto.ProtobufMessageDecoder;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.PostConstruct;
import com.xy.spring.annotations.core.Value;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.IntStream;


/**
 * TCP服务器，支持JSON和Protobuf协议
 */
@Slf4j(topic = LogConstant.Netty)
@Component
public class TCPSocketTemplate extends AbstractRemoteServer {

    @Value("netty.config.tcp.enable")
    protected Boolean tcpEnable;

    @Value("netty.config.tcp.port")
    protected List<Integer> tcpPort;

    @Value("netty.config.bossThreadSize")
    protected Integer bossThreadSize;

    @Value("netty.config.workThreadSize")
    protected Integer workThreadSize;

    // 协议类型配置: json 或 proto
    @Value("netty.config.protocol")
    protected String protocolType;

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


    /**
     * 启动TCP服务器
     */
    public void start() {

        if (!tcpEnable) {
            log.info("TCP服务器未启用");
            return;
        }

        bootstrap = new ServerBootstrap();
        bossGroup = NettyEventLoopFactory.eventLoopGroup(bossThreadSize);
        workGroup = NettyEventLoopFactory.eventLoopGroup(workThreadSize);

        // 设置内存分配器为池化分配器，提升性能
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        // 配置主从线程模型
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

                        // 支持 HTTP/WebSocket 握手（如果你需要）
                        pipeline.addLast(authHandler);

                        // 长度前缀帧：统一对消息做 4 字节 length 前缀
                        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(10485760, 0, 4, 0, 4));
                        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));

                        // 根据配置选择协议类型
                        if ("json".equalsIgnoreCase(protocolType)) {
                            // JSON协议编解码器
                            pipeline.addLast("decoder", new JsonMessageDecoder());
                            pipeline.addLast("encoder", new JsonMessageDecoder());
                            //pipeline.addLast("jsonHandler", new JsonMessageHandler());
                        } else if ("proto".equalsIgnoreCase(protocolType)) {
                            // Protobuf协议编解码器
                            pipeline.addLast("decoder", new ProtobufMessageDecoder());
                            pipeline.addLast("encoder", new ProtobufMessageEncoder());
                            //pipeline.addLast("protoHandler", new ProtobufMessageHandler());
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

        if (CollectionUtil.isEmpty(tcpPort)) {
            log.warn("未配置任何 TCP 端口，启动终止");
            return;
        }

        this.channelFutures = new ChannelFuture[tcpPort.size()];

        IntStream.range(0, tcpPort.size()).parallel().forEach(i -> {

            Integer port = tcpPort.get(i);

            try {

                ChannelFuture future = bootstrap.bind(port).sync();

                channelFutures[i] = future;

                nacosTemplate.registerNacos(port);

                log.info("TCP 端口绑定成功: {}", port);

                future.channel().closeFuture().addListener(cf -> {
                    log.warn("TCP 端口 [{}] 的 Channel 已关闭", port);
                    channelFutures[i] = null;
                });

            } catch (Exception e) {
                log.error("TCP 端口绑定失败: {}", port, e);
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
            log.info("TCP 服务已优雅停止");
        } catch (Exception e) {
            log.error("TCP 优雅关闭失败", e);
        }
    }
}