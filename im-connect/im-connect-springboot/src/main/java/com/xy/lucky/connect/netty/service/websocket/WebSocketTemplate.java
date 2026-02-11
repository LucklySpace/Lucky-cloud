package com.xy.lucky.connect.netty.service.websocket;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.connect.nacos.NacosRegistrationService;
import com.xy.lucky.connect.netty.AuthHandler;
import com.xy.lucky.connect.netty.factory.NettyEventLoopFactory;
import com.xy.lucky.connect.netty.service.AbstractRemoteServer;
import com.xy.lucky.connect.netty.service.websocket.codec.json.JsonMessageHandler;
import com.xy.lucky.connect.netty.service.websocket.codec.proto.ProtobufMessageHandler;
import com.xy.lucky.connect.utils.IPAddressUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务（基于 Netty）
 * - 支持多端口绑定
 * - 支持协议切换（json / proto）
 * - 异步启动，不阻塞主线程
 * - 使用 @ConfigurationProperties 配置类注入配置
 */
@Slf4j(topic = LogConstant.Netty)
@Component
public class WebSocketTemplate extends AbstractRemoteServer implements ApplicationRunner {

    // 静态初始化（类加载时执行，一次性）
    private static final Map<String, ChannelOutboundHandler> PROTOCOL_MAP;

    static {
        JsonMessageHandler jsonMessageHandler = new JsonMessageHandler();
        ProtobufMessageHandler protobufMessageHandler = new ProtobufMessageHandler();
        PROTOCOL_MAP = Map.of("json", jsonMessageHandler, "proto", protobufMessageHandler);
    }

    // 并发 Map：端口 -> ChannelFuture，便于运行时管理单端口的关闭/重绑定
    private final ConcurrentHashMap<Integer, ChannelFuture> channelFutures = new ConcurrentHashMap<>();

    // 使用配置类注入配置
    @Autowired
    private NettyProperties nettyProperties;

    @Value("${brokerId:}")
    private String brokerId;

    @Autowired
    private AuthHandler authHandler;

    @Autowired
    private NacosRegistrationService nacosRegistrationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 异步启动，不阻塞主线程
        CompletableFuture.runAsync(this::startAsync)
                .exceptionally(throwable -> {
                    log.error("WebSocket 异步启动失败", throwable);
                    return null;
                });
    }

    private synchronized void startAsync() {
        // 避免重复启动
        if (ready.get()) {
            log.warn("WebSocket 已经处于运行状态，忽略重复启动请求");
            return;
        }

        // 从配置类获取配置
        NettyProperties.WebSocketConfig wsConfig = nettyProperties.getWebsocket();

        // 基本配置校验
        if (!wsConfig.isEnable()) {
            log.info("WebSocket 未启用（配置 netty.config.websocket.enable=false）");
            return;
        }
        List<Integer> webSocketPort = wsConfig.getPort();
        if (webSocketPort == null || webSocketPort.isEmpty()) {
            log.warn("未配置任何 WebSocket 端口，启动终止");
            return;
        }

        // 初始化 Netty
        bootstrap = new ServerBootstrap();

        bossGroup = NettyEventLoopFactory.eventLoopGroup(nettyProperties.getBossThreadSize());

        workerGroup = NettyEventLoopFactory.eventLoopGroup(nettyProperties.getWorkThreadSize());

        // 对象池优化（PooledByteBufAllocator）
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        bootstrap.group(bossGroup, workerGroup)
                // 设置服务端NIO通信类型
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                // 设置 TCP 参数，SO_BACKLOG 表示队列大小，用于处理临时的高并发连接请求，合理设置能避免拒绝服务
                .option(ChannelOption.SO_BACKLOG, 1024)
                // 是否允许重用 Socket 地址，避免某些情况下的端口占用问题
                .option(ChannelOption.SO_REUSEADDR, true)
                // 接收缓冲区大小，根据需要调整，以减少大流量情况下数据包丢失的风险
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                // 是否开启 TCP 底层心跳机制 保持长连接状态，避免连接频繁断开重连
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // TCP 默认开启 Nagle 算法，该算法的作用是尽可能发送大数据块，减少网络传输。TCP_NODELAY 参数控制是否启用 Nagle 算法；禁用 Nagle 算法，减少延迟，提高实时性
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 设置 ChannelPipeline，也就是业务职责链，由处理的 Handler 串联而成，由 worker 线程池处理
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        String wsPath = nettyProperties.getWebsocket().getPath();
                        String protocolType = nettyProperties.getProtocol();

                        // HTTP 编解码及聚合
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(1024 * 128));
                        pipeline.addLast("chunked", new ChunkedWriteHandler());

                        // 权限/鉴权（自定义 handler）
                        pipeline.addLast("auth", authHandler);

                        // WebSocket 协议处理
                        pipeline.addLast("ws-protocol", new WebSocketServerProtocolHandler(wsPath, protocolType, true,
                                65536 * 10));

                        ChannelOutboundHandler handler =
                                PROTOCOL_MAP.getOrDefault(protocolType, PROTOCOL_MAP.get("proto"));

                        // 协议编解码器
                        pipeline.addLast("protocol", handler);

                        log.info("序列化协议：{}", protocolType);

                        if (!PROTOCOL_MAP.containsKey(protocolType)) {
                            log.warn("未知协议类型: {}, 使用默认 proto", protocolType);
                        }
                    }
                });

        // 绑定端口（在同一线程按序启动多个端口）
        try {
            bindPorts();
            ready.set(true);
            log.info("WebSocket 服务器启动完毕 端口: {}", channelFutures.keySet());
        } catch (Exception ex) {
            log.error("WebSocket 服务器启动失败", ex);
            shutdown();
        }
    }

    /**
     * 获取 WebSocket 端口列表
     */
    private List<Integer> getWebSocketPorts() {
        return nettyProperties.getWebsocket().getPort();
    }

    /**
     * 验证单个端口是否可用（使用 IPAddressUtil 提前探测），但不在此强制退出
     * 建议：真实绑定成功才代表端口可用，这里主要做提示与早期校验
     */
    private void validatePortListOrThrow() {
        List<Integer> ports = getWebSocketPorts();
        for (Integer port : ports) {
            if (port == null) {
                throw new IllegalArgumentException("webSocketPort 包含 null 值");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("非法端口: " + port);
            }
            // 如果检测到端口已被占用，记录并抛出异常（避免直接 System.exit）
            if (!IPAddressUtil.isPortAvailable(port)) {
                throw new IllegalStateException("端口已被占用: " + port);
            }
        }
    }

    /**
     * 逐个绑定配置的端口。每个端口绑定成功后会保存 ChannelFuture 到 channelFutures 中并注册 close 回调清理
     */
    private void bindPorts() {
        // 先做基础校验（抛出异常由上层统一处理）
        validatePortListOrThrow();

        List<Integer> webSocketPort = getWebSocketPorts();
        for (Integer port : webSocketPort) {
            if (channelFutures.containsKey(port)) {
                log.info("端口 {} 已存在绑定记录，跳过", port);
                continue;
            }
            try {
                ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
                channelFutures.put(port, future);
                log.info("WebSocket 端口绑定成功: {}", port);

                // 添加 close future 监听，channel 关闭时清理 map
                future.channel().closeFuture().addListener((ChannelFutureListener) cf -> {
                    log.warn("WebSocket 端口 [{}] 的 Channel 已关闭", port);
                    channelFutures.remove(port);
                });

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("绑定端口被中断 {}", port, ie);
            } catch (Exception e) {
                // 绑定失败，记录但继续尝试下一个端口
                log.error("WebSocket 端口绑定失败: {}", port, e);
            }
        }

        // 批量注册到 Nacos（仅注册实际绑定成功的端口）
        try {
            nacosRegistrationService.registerWebsocketPorts(List.copyOf(channelFutures.keySet()));
        } catch (Exception e) {
            log.error("WebSocket 端口注册到 Nacos 失败", e);
        }

        if (channelFutures.isEmpty()) {
            throw new IllegalStateException("未能绑定任何 WebSocket 端口，应用无法提供 WS 服务");
        }
    }

    /**
     * 关闭 Netty 线程组与已绑定的 channel
     */
    @PreDestroy
    public synchronized void shutdown() {
        if (!ready.get()) {
            log.info("Netty 未运行或已停止");
        } else {
            log.info("正在关闭 Netty WebSocket 服务...");
        }

        try {
            // 先关闭所有 channel
            for (Map.Entry<Integer, ChannelFuture> e : channelFutures.entrySet()) {
                ChannelFuture f = e.getValue();
                if (f != null && f.channel().isOpen()) {
                    try {
                        f.channel().close().syncUninterruptibly();
                        log.info("已关闭 port={}", e.getKey());
                    } catch (Exception ex) {
                        log.warn("关闭 channel(port={}) 时异常", e.getKey(), ex);
                    }
                }
            }
            channelFutures.clear();

            // 优雅停止 event loop group（等待其完成）
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }

            ready.set(false);
            log.info("Netty WebSocket 服务器已关闭");
        } catch (Exception e) {
            log.error("netty 关闭失败", e);
        }
    }

    // 可选扩展：运行时关闭某端口（便于热更新/下线）
    public boolean closePort(int port) {
        ChannelFuture f = channelFutures.get(port);
        if (f == null) {
            log.warn("尝试关闭未绑定的端口: {}", port);
            return false;
        }
        try {
            f.channel().close().syncUninterruptibly();
            channelFutures.remove(port);
            log.info("已关闭端口 {}", port);
            return true;
        } catch (Exception e) {
            log.error("关闭端口失败: {}", port, e);
            return false;
        }
    }

    // 可选扩展：运行时绑定新端口（简单示意）
    public boolean bindNewPort(int port) {
        if (channelFutures.containsKey(port)) {
            log.warn("端口 {} 已存在绑定", port);
            return false;
        }
        try {
            ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
            channelFutures.put(port, future);
            future.channel().closeFuture().addListener((ChannelFutureListener) cf -> channelFutures.remove(port));
            log.info("动态绑定端口成功 {}", port);
            return true;
        } catch (Exception e) {
            log.error("动态绑定端口失败 {}", port, e);
            return false;
        }
    }

    // 可提供一些运行时状态查询方法（例如：isReady / getBoundPorts）
    public boolean isReady() {
        return ready.get();
    }

    public List<Integer> getBoundPorts() {
        return List.copyOf(channelFutures.keySet());
    }


}
