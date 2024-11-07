package com.xy.meet.netty.service;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.meet.config.ConfigCenter;
import com.xy.meet.config.IMNacosConfig;
import com.xy.meet.config.IMNettyConfig;
import com.xy.meet.config.LogConstant;
import com.xy.meet.netty.IMeetChatServerHandler;
import com.xy.meet.netty.service.codec.MessageDecoder;
import com.xy.meet.netty.service.codec.MessageEncoder;
import com.xy.meet.utils.IPAddressUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = LogConstant.NETTY)
public class IMeetChatServer {

    IMNettyConfig.NettyConfig nettyConfig;
    IMNacosConfig.NacosConfig nacosConfig;

    public IMeetChatServer() {
        this.nettyConfig = ConfigCenter.nettyConfig.getNettyConfig();
        this.nacosConfig = ConfigCenter.nacosConfig.getNacosConfig();
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加HTTP编解码器
                            pipeline.addLast("http-codec", new HttpServerCodec());
                            // 聚合HTTP消息，避免处理分段数据
                            pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                            // 支持大数据流的处理
                            pipeline.addLast("http-chunked", new ChunkedWriteHandler());

                            // 处理WebSocket握手和数据帧的协议管理
                            pipeline.addLast(new WebSocketServerProtocolHandler("/meet"));
                            // 编码器，将消息对象转为WebSocket数据帧
                            pipeline.addLast("encode", new MessageEncoder());
                            // 解码器，将WebSocket数据帧转为消息对象
                            pipeline.addLast("decode", new MessageDecoder());

                            // 添加心跳检测
                            pipeline.addLast(new IdleStateHandler(0, 0,nettyConfig.getHeartBeatTime(), TimeUnit.MILLISECONDS));

                            pipeline.addLast(new IMeetChatServerHandler());
                        }
                    });

            List<Integer> portList = nettyConfig.getWebSocketConfig().getPort();

            for (Integer port : portList) {
                registerNacos(port);
                ChannelFuture future = bootstrap.bind(port).sync();
                System.out.println("Server started on port: " + port);
                future.channel().closeFuture().sync();
            }

        } catch (Exception e) {

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
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
            //instance.addMetadata("broker_id", BROKERID); // 机器码
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
