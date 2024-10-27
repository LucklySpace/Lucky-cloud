package com.xy.connect.netty.service.websocket;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.HttpRequestHandler;
import com.xy.connect.netty.factory.NettyEventLoopFactory;
import com.xy.connect.netty.service.AbstractRemoteServer;
import com.xy.connect.netty.service.websocket.codec.MessageDecoder;
import com.xy.connect.netty.service.websocket.codec.MessageEncoder;
import com.xy.connect.utils.IPAddressUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.xy.connect.StartCenter.BROKERID;


/**
 * WS服务器
 */
@Slf4j(topic = LogConstant.NETTY)
public class WebSocketServer extends AbstractRemoteServer {

    private ChannelFuture[] ChannelFutures = null;

    @Override
    public void start() {
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
                // 保持长连接状态，避免连接频繁断开重连
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 禁用Nagle算法，减少延迟，提高实时性
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 设置ChannelPipeline，也就是业务职责链，由处理的Handler串联而成，由worker线程池处理
                .childHandler(new ChannelInitializer<Channel>() {

                    // 初始化每个Channel的职责链,添加处理的Handler，通常包括消息编解码、业务处理，也可以是日志、权限、过滤等
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加HTTP编解码器
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        // 聚合HTTP消息，避免处理分段数据
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                        // 支持大数据流的处理
                        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                        // 处理Http请求转WebSocket握手请求
                        pipeline.addLast(new HttpRequestHandler());
                        // 处理WebSocket握手和数据帧的协议管理
                        pipeline.addLast(new WebSocketServerProtocolHandler("/im"));
                        // 编码器，将消息对象转为WebSocket数据帧
                        pipeline.addLast("encode", new MessageEncoder());
                        // 解码器，将WebSocket数据帧转为消息对象
                        pipeline.addLast("decode", new MessageDecoder());
                    }
                });
        // 绑定端口
        bindPort();
    }

    public void bindPort() {
        try {
            List<Integer> ports = nettyConfig.getWebSocketConfig().getPort();

            if (ChannelFutures == null) {
                ChannelFutures = new ChannelFuture[ports.size()];
            }

            for (int i = 0; i < ports.size(); i++) {
                Integer port = ports.get(i);
                ChannelFuture channelFuture = bootstrap.bind(port);
                ChannelFutures[i] = channelFuture;
                channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        if (future.isSuccess()) {
                            // 注册服务到nacos
                            registerNacos(port);
                            log.info("Started success,port:{}", port);
                        } else {
                            log.info("Started Failed,port:{}", port);
                        }
                    }
                });
            }


            for (int i = 0; i < ports.size(); i++) {
                final Channel channel = ChannelFutures[i].channel();
                int finalI = i;
                channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) {
                        log.info("channel close !");
                        channel.close();
                        ChannelFutures[finalI] = null;
                    }
                });
            }

            // 就绪标志
            this.ready = true;

            log.info("WebSocket server initialization completed on ports: {}", ports);
        } catch (Exception e) {
            log.error("WebSocket server initialization failed", e);
        }
    }

    /**
     * 注册服务到nacos
     * https://juejin.cn/post/6844903782086606861
     *
     * @param port 端口
     */
    public void registerNacos(Integer port) {
        try {
            // 获取Nacos服务地址
            String serverIp = nacosConfig.getAddress();
            Integer serverPort = nacosConfig.getPort();
            String serviceName = nacosConfig.getName();
            String version = nacosConfig.getVersion();
            String serverAddr = serverIp + ":" + serverPort;

            // 获取本机IP地址
            String ip = IPAddressUtil.getLocalIp4Address();

            // 创建Nacos实例
            Instance instance = new Instance();
            instance.setIp(ip); // IP地址
            instance.setPort(port); // 端口
            instance.setServiceName(serviceName); // 服务名
            instance.setEnabled(true); // 是否启用
            instance.setHealthy(true); // 健康状态
            instance.setWeight(1.0); // 权重
            instance.addMetadata("broker_id", BROKERID); // 机器码
            instance.addMetadata("version", version); // 版本号
            instance.addMetadata("protocol", "websocket"); // 协议
            // 注册服务到Nacos
            NamingService namingService = NamingFactory.createNamingService(serverAddr);
            namingService.registerInstance(serviceName, instance);
            log.info("Service registered to Nacos successfully, port: {}", port);
        } catch (Exception e) {
            log.error("Failed to register service to Nacos, port: {}", port, e);
        }
    }

}
