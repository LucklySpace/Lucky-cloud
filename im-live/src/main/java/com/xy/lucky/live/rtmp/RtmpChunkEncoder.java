package com.xy.lucky.live.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * RTMP Chunk 编码器（极简演示版）
 * - 始终使用 fmt=0 的完整消息头，并将消息一次性写出（不分片）
 * - 构造 BasicHeader(0,csid=3) + MessageHeader(11) + payload
 */
public class RtmpChunkEncoder extends ChannelOutboundHandlerAdapter {
    private static final int DEFAULT_CSID = 3;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof RtmpMessage rm)) {
            ctx.write(msg, promise);
            return;
        }
        ByteBuf out = Unpooled.buffer(1 + 11 + rm.payload.readableBytes());
        int basic = (0 << 6) | (DEFAULT_CSID & 0x3F);
        out.writeByte(basic);
        // timestamp (3 bytes)
        out.writeByte((rm.timestamp >> 16) & 0xFF);
        out.writeByte((rm.timestamp >> 8) & 0xFF);
        out.writeByte(rm.timestamp & 0xFF);
        // message length (3 bytes)
        int len = rm.payload.readableBytes();
        out.writeByte((len >> 16) & 0xFF);
        out.writeByte((len >> 8) & 0xFF);
        out.writeByte(len & 0xFF);
        // type id
        out.writeByte(rm.messageTypeId & 0xFF);
        // message stream id (little-endian)
        out.writeInt(Integer.reverseBytes(rm.messageStreamId));
        // payload
        out.writeBytes(rm.payload, rm.payload.readerIndex(), rm.payload.readableBytes());
        ctx.write(out, promise);
    }
}

