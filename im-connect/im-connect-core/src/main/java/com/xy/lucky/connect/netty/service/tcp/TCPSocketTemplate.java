package com.xy.lucky.connect.netty.service.tcp;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.connect.nacos.NacosTemplate;
import com.xy.lucky.connect.netty.AuthHandler;
import com.xy.lucky.connect.netty.factory.NettyEventLoopFactory;
import com.xy.lucky.connect.netty.service.AbstractRemoteServer;
import com.xy.lucky.connect.netty.service.tcp.codec.json.TcpJsonMessageHandler;
import com.xy.lucky.connect.utils.IPAddressUtil;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.annotations.core.Value;
import com.xy.lucky.spring.boot.context.ApplicationArguments;
import com.xy.lucky.spring.boot.context.ApplicationRunner;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP 服务器模板（支持 JSON / Protobuf 协议）
 * <p>
 * - 支持多端口绑定
 * - 支持协议切换（json / proto）
 * - 异步启动，不阻塞主线程
 * - 使用长度字段帧解码器实现粘包拆包
 * <p>
 * 协议格式：
 * +--------+----------------+
 * | Length |    Payload     |
 * | 4 bytes|   N bytes      |
 * +--------+----------------+
 */
@Slf4j(topic = LogConstant.Netty)
@Component
public class TCPSocketTemplate extends AbstractRemoteServer implements ApplicationRunner {

    // 静态初始化协议处理器
    private static final Map<String, ChannelHandler> PROTOCOL_MAP;

    static {
        TcpJsonMessageHandler jsonHandler = new TcpJsonMessageHandler();
        PROTOCOL_MAP = Map.of("json", jsonHandler);
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
    private NacosTemplate nacosTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 异步启动，不阻塞主线程
        CompletableFuture.runAsync(this::startAsync)
                .exceptionally(throwable -> {
                    log.error("TCP Server 异步启动失败", throwable);
                    return null;
                });
    }

    private synchronized void startAsync() {
        // 避免重复启动
        if (ready.get()) {
            log.warn("TCP Server 已经处于运行状态，忽略重复启动请求");
            return;
        }

        // 从配置类获取配置
        NettyProperties.TcpConfig tcpConfig = nettyProperties.getTcp();

        // 基本配置校验
        if (!tcpConfig.isEnable()) {
            log.info("TCP 服务器未启用（配置 netty.config.tcp.enable=false ）");
            return;
        }
        List<Integer> tcpPorts = tcpConfig.getPort();
        if (tcpPorts == null || tcpPorts.isEmpty()) {
            log.warn("未配置任何 TCP 端口，启动终止");
            return;
        }

        // 初始化 Netty
        bootstrap = new ServerBootstrap();

        bossGroup = NettyEventLoopFactory.eventLoopGroup(nettyProperties.getBossThreadSize());
        workerGroup = NettyEventLoopFactory.eventLoopGroup(nettyProperties.getWorkThreadSize());

        // 对象池优化
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        bootstrap.group(bossGroup, workerGroup)
                // 设置服务端 NIO 通信类型
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                // TCP 参数配置
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                // 子 Channel 配置
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        String protocolType = nettyProperties.getProtocol();

                        // 帧解码器：处理 TCP 粘包/拆包
                        // 参数说明：
                        // - maxFrameLength: 最大帧长度 10MB
                        // - lengthFieldOffset: 长度字段偏移量 0
                        // - lengthFieldLength: 长度字段本身长度 4 字节
                        // - lengthAdjustment: 长度调整值 0
                        // - initialBytesToStrip: 解码后跳过的字节数 4（跳过长度字段）
                        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                                10 * 1024 * 1024, 0, 4, 0, 4));

                        // 帧编码器：在发送数据前添加 4 字节长度头
                        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));

                        // 权限/鉴权（自定义 handler）
                        pipeline.addLast("auth", authHandler);

                        // 协议编解码器（JSON 或 Protobuf）
                        ChannelHandler handler = PROTOCOL_MAP.getOrDefault(protocolType, PROTOCOL_MAP.get("json"));
                        pipeline.addLast("protocol", handler);

                        log.debug("TCP Channel 初始化完成, 序列化协议：{}", protocolType);

                        if (!PROTOCOL_MAP.containsKey(protocolType)) {
                            log.warn("未知协议类型: {}, 使用默认 json", protocolType);
                        }
                    }
                });

        // 绑定端口
        try {
            bindPorts();
            ready.set(true);
            log.info("TCP 服务器启动完成, 端口: {}", channelFutures.keySet());
        } catch (Exception ex) {
            log.error("TCP 服务器启动失败", ex);
            shutdown();
        }
    }

    /**
     * 获取 TCP 端口列表
     */
    private List<Integer> getTcpPorts() {
        return nettyProperties.getTcp().getPort();
    }

    /**
     * 验证端口是否可用
     */
    private void validatePortListOrThrow() {
        List<Integer> ports = getTcpPorts();
        for (Integer port : ports) {
            if (port == null) {
                throw new IllegalArgumentException("tcpPort 包含 null 值");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("非法端口值: " + port);
            }
            if (!IPAddressUtil.isPortAvailable(port)) {
                throw new IllegalStateException("端口已被占用: " + port);
            }
        }
    }

    /**
     * 逐个绑定配置的端口
     */
    private void bindPorts() {
        // 先做基础校验
        validatePortListOrThrow();

        List<Integer> tcpPorts = getTcpPorts();
        for (Integer port : tcpPorts) {
            if (channelFutures.containsKey(port)) {
                log.info("端口 {} 已存在绑定记录，跳过", port);
                continue;
            }
            try {
                ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
                channelFutures.put(port, future);
                log.info("TCP 端口绑定成功: {}", port);

                // 添加 close future 监听，channel 关闭时清理 map
                future.channel().closeFuture().addListener((ChannelFutureListener) cf -> {
                    log.warn("TCP 端口 [{}] 的 Channel 已关闭", port);
                    channelFutures.remove(port);
                });

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("绑定端口被中断: {}", port, ie);
            } catch (Exception e) {
                log.error("TCP 端口绑定失败: {}", port, e);
            }
        }

        // 批量注册到 Nacos（可选，TCP 服务也可以注册）
        // nacosTemplate.batchRegisterNacos(tcpPorts);

        if (channelFutures.isEmpty()) {
            throw new IllegalStateException("未能绑定任何 TCP 端口，应用无法提供 TCP 服务");
        }
    }

    /**
     * 关闭 Netty 线程组与已绑定的 channel
     */
    @PreDestroy
    public synchronized void shutdown() {
        if (!ready.get()) {
            log.info("TCP Server 未运行或已停止");
        } else {
            log.info("正在关闭 TCP 服务器...");
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

            // 优雅停止 event loop group
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().syncUninterruptibly();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().syncUninterruptibly();
            }

            ready.set(false);
            log.info("TCP 服务器已关闭");
        } catch (Exception e) {
            log.error("TCP 服务器关闭失败", e);
        }
    }

    /**
     * 运行时关闭某端口
     */
    public boolean closePort(int port) {
        ChannelFuture f = channelFutures.get(port);
        if (f == null) {
            log.warn("尝试关闭未绑定的端口: {}", port);
            return false;
        }
        try {
            f.channel().close().syncUninterruptibly();
            channelFutures.remove(port);
            log.info("已关闭端口: {}", port);
            return true;
        } catch (Exception e) {
            log.error("关闭端口失败: {}", port, e);
            return false;
        }
    }

    /**
     * 运行时绑定新端口
     */
    public boolean bindNewPort(int port) {
        if (channelFutures.containsKey(port)) {
            log.warn("端口 {} 已存在绑定", port);
            return false;
        }
        try {
            ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
            channelFutures.put(port, future);
            future.channel().closeFuture().addListener((ChannelFutureListener) cf -> channelFutures.remove(port));
            log.info("动态绑定端口成功: {}", port);
            return true;
        } catch (Exception e) {
            log.error("动态绑定端口失败: {}", port, e);
            return false;
        }
    }

    /**
     * 查询服务是否就绪
     */
    public boolean isReady() {
        return ready.get();
    }

    /**
     * 获取已绑定的端口列表
     */
    public List<Integer> getBoundPorts() {
        return List.copyOf(channelFutures.keySet());
    }
}
