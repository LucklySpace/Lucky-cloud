package com.xy.connect.netty.service.tcp;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.HttpRequestHandler;
import com.xy.connect.netty.factory.NettyEventLoopFactory;
import com.xy.connect.netty.service.AbstractRemoteServer;
import com.xy.connect.netty.service.tcp.codec.MessageDecoder;
import com.xy.connect.netty.service.tcp.codec.MessageEncoder;
import com.xy.connect.utils.IPAddressUtil;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.xy.connect.StartCenter.BROKERID;


/**
 * TCP服务器
 */
@Slf4j(topic = LogConstant.NETTY)
public class TcpSocketServer extends AbstractRemoteServer {

    private ChannelFuture[] ChannelFutures = null;

    @Override
    public void start() {
        // 设置为主从线程模型
        bootstrap.group(bossGroup, workGroup)
                // 设置服务端NIO通信类型
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, false)
                // 表示连接保活，相当于心跳机制，默认为7200s
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 设置ChannelPipeline，也就是业务职责链，由处理的Handler串联而成，由从线程池处理
                .childHandler(new ChannelInitializer<Channel>() {

                    // 添加处理的Handler，通常包括消息编解码、业务处理，也可以是日志、权限、过滤等
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        // 获取职责链
                        ChannelPipeline pipeline = ch.pipeline();

                        // 处理Http请求转WebSocket握手请求
                        pipeline.addLast(new HttpRequestHandler());
                        // 添加消息编解码器
                        pipeline.addLast("encode", new MessageEncoder());
                        pipeline.addLast("decode", new MessageDecoder());


                        // pipeline.addLast(new IdleStateHandler(1, 1, 1, TimeUnit.MINUTES)); // 设置读、写、空闲状态超时时间为1分钟
//                        pipeline.addLast("handler", new IMChannelHandler());
                        //addPipeline(pipeline);
                    }
                });
        // 绑定端口
        bindPort();
    }

    public void bindPort() {
        try {
            List<Integer> ports = nettyConfig.getTcpConfig().getPort();

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
            instance.addMetadata("brokerId", BROKERID); // 机器码
            instance.addMetadata("version", version); // 版本号
            instance.addMetadata("protocol", "tcp"); // 协议

            // 注册服务到Nacos
            NamingService namingService = NamingFactory.createNamingService(serverAddr);
            namingService.registerInstance(serviceName, instance);
            log.info("Service registered to Nacos successfully, port: {}", port);
        } catch (Exception e) {
            log.error("Failed to register service to Nacos, port: {}", port, e);
        }
    }

}
