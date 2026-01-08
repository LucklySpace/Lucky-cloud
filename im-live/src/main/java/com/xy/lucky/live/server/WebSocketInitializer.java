package com.xy.lucky.live.server;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.signal.SignalDispatcher;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket Channel 初始化器
 * <p>
 * 配置 Netty Pipeline，添加必要的编解码器和处理器
 *
 * @author lucky
 */
public class WebSocketInitializer extends ChannelInitializer<SocketChannel> {

    private final LiveProperties liveProperties;
    private final SignalDispatcher signalDispatcher;

    public WebSocketInitializer(LiveProperties liveProperties, SignalDispatcher signalDispatcher) {
        this.liveProperties = liveProperties;
        this.signalDispatcher = signalDispatcher;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        LiveProperties.SignalingConfig config = liveProperties.getSignaling();

        // HTTP 编解码器
        pipeline.addLast("http-codec", new HttpServerCodec());

        // HTTP 消息聚合器
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(config.getMaxFrameSize()));

        // 分块写入处理器
        pipeline.addLast("chunked-write", new ChunkedWriteHandler());

        // WebSocket 压缩扩展（可选）
        pipeline.addLast("ws-compression", new WebSocketServerCompressionHandler());

        // 空闲检测处理器
        // readerIdleTime: 读空闲超时时间
        // writerIdleTime: 写空闲超时时间
        // allIdleTime: 读写空闲超时时间
        long heartbeatTimeout = config.getHeartbeatTimeout();
        pipeline.addLast("idle-state", new IdleStateHandler(
                heartbeatTimeout, 0, 0, TimeUnit.MILLISECONDS));

        // WebSocket 协议处理器
        // 处理握手、Ping/Pong、Close 帧
        pipeline.addLast("ws-protocol", new WebSocketServerProtocolHandler(
                config.getPath(),           // WebSocket 路径
                null,                       // 子协议
                true,                       // 允许扩展
                config.getMaxFrameSize(),   // 最大帧大小
                false,                      // 是否检查请求路径起始斜杠
                true                        // 处理 Close 帧时是否验证 UTF-8
        ));

        // 业务处理器
        pipeline.addLast("ws-handler", new WebSocketHandler(signalDispatcher));
    }
}

