package com.xy.lucky.live.rtmp;

import io.netty.buffer.ByteBuf;

/**
 * RTMP 消息模型（简化）
 * - 记录类型（audio=8, video=9, command=20/17 等）
 * - 记录消息体
 */
public final class RtmpMessage {
    public final int messageTypeId;
    public final int messageStreamId;
    public final int timestamp;
    public final ByteBuf payload;

    public RtmpMessage(int messageTypeId, int messageStreamId, int timestamp, ByteBuf payload) {
        this.messageTypeId = messageTypeId;
        this.messageStreamId = messageStreamId;
        this.timestamp = timestamp;
        this.payload = payload;
    }
}

