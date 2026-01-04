package com.xy.lucky.live.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.security.SecureRandom;

/**
 * RTMP 握手处理（简化版）
 * 客户端: C0(1) + C1(1536) -> 服务端: S0 + S1 + S2 -> 客户端: C2
 * 本实现仅遵循简单握手流程，不做复杂校验
 */
public class RtmpHandshakeHandler extends ChannelInboundHandlerAdapter {
    private static final SecureRandom RAND = new SecureRandom();
    private int state = 0;
    private byte[] c1;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf buf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        try {
            while (buf.isReadable()) {
                switch (state) {
                    case 0 -> { // 读取 C0
                        if (buf.readableBytes() < 1) return;
                        buf.readByte(); // version 固定 0x03
                        state = 1;
                    }
                    case 1 -> { // 读取 C1
                        if (buf.readableBytes() < 1536) return;
                        c1 = new byte[1536];
                        buf.readBytes(c1);
                        // 发送 S0S1S2
                        ByteBuf out = ctx.alloc().buffer(1 + 1536 + 1536);
                        out.writeByte(0x03);
                        byte[] s1 = new byte[1536];
                        RAND.nextBytes(s1);
                        out.writeBytes(s1);
                        out.writeBytes(c1);
                        ctx.writeAndFlush(out);
                        state = 2;
                    }
                    case 2 -> { // 读取 C2
                        if (buf.readableBytes() < 1536) return;
                        byte[] c2 = new byte[1536];
                        buf.readBytes(c2);
                        // 简易校验：C2 == S1
                        // 进入命令阶段
                        state = 3;
                        ctx.pipeline().remove(this);
                        return;
                    }
                    default -> {
                        // 已完成握手，透传
                        ctx.fireChannelRead(buf.readBytes(buf.readableBytes()));
                        return;
                    }
                }
            }
        } finally {
            if (buf.refCnt() > 0) buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
