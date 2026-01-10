//package com.xy.lucky.connect.netty.service.tcp;
//
//import com.xy.lucky.connect.config.LogConstant;
//import com.xy.lucky.connect.domain.ImPair;
//import com.xy.lucky.connect.nacos.NacosTemplate;
//import com.xy.lucky.connect.netty.AuthHandler;
//import com.xy.lucky.connect.netty.factory.NettyEventLoopFactory;
//import com.xy.lucky.connect.netty.service.AbstractRemoteServer;
//import com.xy.lucky.connect.netty.service.websocket.codec.json.JsonMessageDecoder;
//import com.xy.lucky.connect.netty.service.websocket.codec.json.JsonMessageEncoder;
//import com.xy.lucky.connect.netty.service.websocket.codec.proto.ProtobufMessageDecoder;
//import com.xy.lucky.connect.netty.service.websocket.codec.proto.ProtobufMessageEncoder;
//import com.xy.lucky.spring.annotations.core.*;
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.buffer.PooledByteBufAllocator;
//import io.netty.channel.*;
//import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
//import io.netty.handler.codec.LengthFieldPrepender;
//import lombok.extern.slf4j.Slf4j;
//
//import java.net.InetSocketAddress;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CompletableFuture;
//import java.util.function.Supplier;
//
//
/// **
// * TCP 服务器模板（支持 JSON / Protobuf 协议）
// * 异步启动，不阻塞主线程
// */
//@Slf4j(topic = LogConstant.Netty)
//@Component
//public class TCPSocketTemplate extends AbstractRemoteServer {
//
//    private static final Map<String, Supplier<ImPair<ChannelInboundHandler, ChannelOutboundHandler>>> PROTOCOL_MAP =
//            Map.of(
//                    "json", () -> new ImPair<>(new JsonMessageDecoder(), new JsonMessageEncoder()),
//                    "proto", () -> new ImPair<>(new ProtobufMessageDecoder(), new ProtobufMessageEncoder())
//            );
//    // 并发 map 管理端口 -> ChannelFuture，便于运行时操作单端口
//    private final ConcurrentHashMap<Integer, ChannelFuture> channelFutures = new ConcurrentHashMap<>();
//    // 建议替换为 @ConfigurationProperties(prefix = "netty.config")
//    @Value("${netty.config.tcp.enable:true}")
//    protected Boolean tcpEnable;
//    @Value("${netty.config.tcp.port:#{null}}")
//    protected List<Integer> tcpPort;
//    @Value("${netty.config.bossThreadSize:1}")
//    protected Integer bossThreadSize;
//    @Value("${netty.config.workThreadSize:0}")
//    protected Integer workThreadSize;
//    @Value("${netty.config.protocol:proto}")
//    protected String protocolType = "proto";
//    @Autowired
//    private AuthHandler authHandler;
//    @Autowired
//    private NacosTemplate nacosTemplate;
//
//    @PostConstruct
//    public void start() {
//        // 异步启动，不阻塞主线程
//        CompletableFuture.runAsync(this::startAsync)
//                .exceptionally(throwable -> {
//                    log.error("TCP Server 异步启动失败", throwable);
//                    return null;
//                });
//    }
//
//    private synchronized void startAsync() {
//        // 避免重复启动
//        if (ready.get()) {
//            log.warn("TCP Server 已运行，忽略重复启动请求");
//            return;
//        }
//
//        if (tcpEnable == null || !tcpEnable) {
//            log.info("TCP 服务器未启用（配置 netty.config.tcp.enable=false ）");
//            return;
//        }
//
//        if (tcpPort == null || tcpPort.isEmpty()) {
//            log.warn("未配置任何 TCP 端口，启动终止");
//            return;
//        }
//
//        // 初始化 Netty bootstrap 与线程池
//        bootstrap = new ServerBootstrap();
//        bossGroup = NettyEventLoopFactory.eventLoopGroup(bossThreadSize);
//        workerGroup = NettyEventLoopFactory.eventLoopGroup(workThreadSize);
//
//        bootstrap.group(bossGroup, workerGroup)
//                .channel(NettyEventLoopFactory.serverSocketChannelClass())
//                // Server 级别 option
//                .option(ChannelOption.SO_BACKLOG, 1024)
//                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
//                // child socket option（accepted sockets）
//                .childOption(ChannelOption.SO_KEEPALIVE, true)
//                .childOption(ChannelOption.TCP_NODELAY, true)
//                .childOption(ChannelOption.SO_RCVBUF, 16 * 1024)
//                .childHandler(new ChannelInitializer<Channel>() {
//                    @Override
//                    protected void initChannel(Channel ch) {
//                        ChannelPipeline pipeline = ch.pipeline();
//                        // 鉴权/认证 handler
//                        pipeline.addLast("auth", authHandler);
//                        // 长度前缀编解码（统一协议帧）
//                        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(10 * 1024 * 1024, 0, 4, 0, 4));
//                        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
//
//                        log.info("序列化协议：{}", protocolType);
//                        ImPair<ChannelInboundHandler, ChannelOutboundHandler> handlers =
//                                PROTOCOL_MAP.getOrDefault(protocolType, PROTOCOL_MAP.get("proto")).get();
//
//                        // outbound encoder, inbound decoder
//                        pipeline.addLast("encoder", handlers.getValue());
//                        pipeline.addLast("decoder", handlers.getKey());
//
//                        if (!PROTOCOL_MAP.containsKey(protocolType)) {
//                            log.warn("未知协议类型: {}, 使用默认 proto", protocolType);
//                        }
//                    }
//                });
//
//        // 绑定端口（每个端口独立处理）
//        try {
//            bindPorts();
//            ready.set(true);
//            log.info("TCP 服务器启动完成，端口：{}", channelFutures.keySet());
//        } catch (Exception ex) {
//            log.error("TCP 服务器启动异常，开始清理资源", ex);
//            shutdown();
//        }
//    }
//
//    /**
//     * 逐端口绑定。单个端口绑定失败不会影响其他端口的尝试。
//     * 说明：调用此方法前可做快速探测（IPAddressUtil），但最终以绑定结果为准（探测存在竞态）。
//     */
//    private void bindPorts() {
//        for (Integer port : tcpPort) {
//            if (port == null) {
//                log.warn("配置的端口列表包含 null，跳过");
//                continue;
//            }
//            if (channelFutures.containsKey(port)) {
//                log.info("端口 {} 已经绑定，跳过", port);
//                continue;
//            }
//            if (port < 1 || port > 65535) {
//                log.warn("非法端口值: {}, 跳过", port);
//                continue;
//            }
//
//            try {
//                ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
//                channelFutures.put(port, future);
//                log.info("TCP 端口绑定成功: {}", port);
//
//                // Nacos 注册单独捕获异常，不影响服务继续启动
//                try {
//                    nacosTemplate.registerNacos(port);
//                } catch (Exception e) {
//                    log.error("Nacos 注册失败（port={}），但服务继续运行: {}", port, e.getMessage(), e);
//                }
//
//                // 监听 channel 关闭以做清理
//                int capturedPort = port;
//                future.channel().closeFuture().addListener((ChannelFutureListener) cf -> {
//                    log.warn("TCP 端口 [{}] 的 Channel 已关闭", capturedPort);
//                    channelFutures.remove(capturedPort);
//                });
//
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                log.error("TCP 端口绑定被中断: {}", port, ie);
//            } catch (Exception e) {
//                log.error("TCP 端口绑定失败: {}", port, e);
//            }
//        }
//
//        if (channelFutures.isEmpty()) {
//            throw new IllegalStateException("未能绑定到任何 TCP 端口，服务启动失败");
//        }
//    }
//
//    /**
//     * 优雅关闭：先关闭 channel，再 shutdownGracefully EventLoopGroup
//     */
//    @PreDestroy
//    public synchronized void shutdown() {
//        if (!ready.get()) {
//            log.info("TCP Server 未运行或已停止");
//        } else {
//            log.info("正在关闭 TCP Server...");
//        }
//
//        try {
//            // 关闭所有活跃 channel
//            for (Map.Entry<Integer, ChannelFuture> entry : channelFutures.entrySet()) {
//                ChannelFuture cf = entry.getValue();
//                if (cf != null && cf.channel().isOpen()) {
//                    try {
//                        cf.channel().close().syncUninterruptibly();
//                        log.info("已关闭端口 {}", entry.getKey());
//                    } catch (Exception ex) {
//                        log.warn("关闭端口 {} 失败", entry.getKey(), ex);
//                    }
//                }
//            }
//            channelFutures.clear();
//
//            // 优雅停止线程组
//            if (bossGroup != null) {
//                bossGroup.shutdownGracefully().syncUninterruptibly();
//            }
//            if (workerGroup != null) {
//                workerGroup.shutdownGracefully().syncUninterruptibly();
//            }
//
//            ready.set(false);
//            log.info("TCP Server 已停止");
//        } catch (Exception e) {
//            log.error("TCP Server 关闭异常", e);
//        }
//    }
//
//    /**
//     * 在运行时动态关闭某个已绑定端口
//     *
//     * @param port 端口号
//     * @return 是否成功触发关闭
//     */
//    public boolean closePort(int port) {
//        ChannelFuture cf = channelFutures.get(port);
//        if (cf == null) {
//            log.warn("尝试关闭未绑定的端口: {}", port);
//            return false;
//        }
//        try {
//            cf.channel().close().syncUninterruptibly();
//            channelFutures.remove(port);
//            log.info("已关闭端口: {}", port);
//            return true;
//        } catch (Exception e) {
//            log.error("关闭端口失败: {}", port, e);
//            return false;
//        }
//    }
//
//    /**
//     * 在运行时动态绑定新端口（简单演示）
//     *
//     * @param port 端口号
//     * @return 是否绑定成功
//     */
//    public boolean bindNewPort(int port) {
//        if (channelFutures.containsKey(port)) {
//            log.warn("端口 {} 已存在绑定", port);
//            return false;
//        }
//        if (port < 1 || port > 65535) {
//            log.warn("非法端口: {}", port);
//            return false;
//        }
//        try {
//            ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
//            channelFutures.put(port, future);
//            future.channel().closeFuture().addListener((ChannelFutureListener) cf -> channelFutures.remove(port));
//            try {
//                nacosTemplate.registerNacos(port);
//            } catch (Exception e) {
//                log.error("动态绑定后 Nacos 注册失败: {}", e.getMessage(), e);
//            }
//            log.info("动态绑定端口成功: {}", port);
//            return true;
//        } catch (Exception e) {
//            log.error("动态绑定端口失败: {}", port, e);
//            return false;
//        }
//    }
//
//    // 可提供一些运行时状态查询方法（例如：isReady / getBoundPorts）
//    public boolean isReady() {
//        return ready.get();
//    }
//
//    public List<Integer> getBoundPorts() {
//        return List.copyOf(channelFutures.keySet());
//    }
//}