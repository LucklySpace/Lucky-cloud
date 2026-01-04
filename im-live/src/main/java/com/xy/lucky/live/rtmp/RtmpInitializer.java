package com.xy.lucky.live.rtmp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * RTMP 管线初始化
 * - 握手处理
 * - 命令/控制消息解析
 */
public class RtmpInitializer extends ChannelInitializer<SocketChannel> {
    private final StreamRegistry registry;

    public RtmpInitializer(StreamRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new RtmpHandshakeHandler());
        p.addLast(new RtmpChunkDecoder());
        p.addLast(new RtmpMediaForwarder(registry));
        p.addLast(new RtmpChunkEncoder());
        p.addLast(new RtmpCommandHandler(registry));
    }
}
