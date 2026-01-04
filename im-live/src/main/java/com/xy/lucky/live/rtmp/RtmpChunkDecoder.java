package com.xy.lucky.live.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * RTMP Chunk 解码器（极简演示版）
 * - 仅支持 chunk size 默认 128
 * - 聚合类型为 0 的消息头（11字节）
 * - 识别 messageTypeId 为音视频与命令类型
 * 注意：生产环境需完整实现 chunk 流（fmt 0/1/2/3）与 csid 管理，这里仅用于演示推拉媒体转发流程。
 */
public class RtmpChunkDecoder extends ChannelInboundHandlerAdapter {
    private static final int DEFAULT_CHUNK_SIZE = 128;
    private final Map<Integer, CsState> states = new HashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf in)) {
            ctx.fireChannelRead(msg);
            return;
        }
        try {
            while (in.isReadable()) {
                int basic = in.readUnsignedByte();
                int fmt = (basic & 0xC0) >>> 6;
                int csid = basic & 0x3F;
                if (csid == 0) {
                    // 简化：忽略扩展 csid
                    csid = 64;
                }
                CsState st = states.computeIfAbsent(csid, k -> new CsState());
                st.fmt = fmt;
                st.csid = csid;

                // 仅实现 fmt=0 的完整消息头
                if (fmt == 0) {
                    if (in.readableBytes() < 11) return;
                    int ts = (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8) | in.readUnsignedByte();
                    int len = (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8) | in.readUnsignedByte();
                    int typeId = in.readUnsignedByte();
                    int streamId = Integer.reverseBytes(in.readInt());
                    st.timestamp = ts;
                    st.msgLength = len;
                    st.msgTypeId = typeId;
                    st.msgStreamId = streamId;
                    if (st.agg != null) {
                        ReferenceCountUtil.release(st.agg);
                        st.agg = null;
                    }
                    st.agg = ctx.alloc().buffer(len);
                } else {
                    // 为简化，其他 fmt 直接跳过（演示）
                    return;
                }

                // 读取 chunk 数据
                int remaining = st.msgLength - st.agg.readableBytes();
                int toRead = Math.min(remaining, Math.min(in.readableBytes(), DEFAULT_CHUNK_SIZE));
                if (toRead > 0) {
                    st.agg.writeBytes(in.readBytes(toRead));
                }
                remaining = st.msgLength - st.agg.readableBytes();
                if (remaining == 0) {
                    ByteBuf payload = st.agg;
                    st.agg = null;
                    RtmpMessage rm = new RtmpMessage(st.msgTypeId, st.msgStreamId, st.timestamp, payload);
                    ctx.fireChannelRead(rm);
                } else {
                    // 等待更多 chunk
                    return;
                }
            }
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(in);
            throw e;
        } finally {
            ReferenceCountUtil.safeRelease(in);
        }
    }

    private static final class CsState {
        int fmt;
        int csid;
        int timestamp;
        int msgLength;
        int msgTypeId;
        int msgStreamId;
        ByteBuf agg;
    }
}

