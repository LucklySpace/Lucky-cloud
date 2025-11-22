package com.xy.lucky.meet.netty.service;

import com.xy.lucky.meet.constant.LogConstant;
import com.xy.lucky.meet.nacos.NacosTemplate;
import com.xy.lucky.meet.netty.IMeetChatServerHandler;
import com.xy.lucky.meet.netty.service.codec.MessageDecoder;
import com.xy.lucky.meet.netty.service.codec.MessageEncoder;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.Value;
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
@Component
public class IMeetChatServer {

    @Value("netty.config.websocket.port")
    protected List<Integer> webSocketPort;

    @Value("netty.config.heartBeatTime")
    protected Integer heartBeatTime;

    @Autowired
    private NacosTemplate nacosTemplate;

    @Autowired
    private IMeetChatServerHandler iMeetChatServerHandler;


    @PostConstruct
    private void startNetty() {
        this.start();
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
                            pipeline.addLast(new IdleStateHandler(0, 0, heartBeatTime, TimeUnit.MILLISECONDS));

                            pipeline.addLast(iMeetChatServerHandler);
                        }
                    });

            for (Integer port : webSocketPort) {
                nacosTemplate.registerNacos(port);
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

}
