package com.xy.lucky.live.webrtc;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 信令管线初始化
 * - HTTP 握手
 * - WebSocket 协议升级
 * - 文本帧处理
 */
public class SignalingInitializer extends ChannelInitializer<SocketChannel> {
    private final RoomService roomService;
    private final java.util.concurrent.ExecutorService executor;

    public SignalingInitializer(RoomService roomService, java.util.concurrent.ExecutorService executor) {
        this.roomService = roomService;
        this.executor = executor;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(64 * 1024));
        p.addLast(new ChunkedWriteHandler());
        p.addLast(new WebSocketServerProtocolHandler("/ws", null, true));
        p.addLast(new SignalingHandler(roomService, executor));
    }
}
