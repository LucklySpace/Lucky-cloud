package com.xy.connect.netty.service.tcp.codec.proto;

import com.xy.connect.domain.proto.IMessageProto;
import com.xy.connect.utils.ProtoJsonUtils;
import com.xy.core.model.IMessageWrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class ProtobufMessageEncoder extends MessageToMessageEncoder<IMessageWrap<?>> {

    @Override
    protected void encode(ChannelHandlerContext ctx, IMessageWrap<?> msg, List<Object> out) throws Exception {

        IMessageProto.IMessageWrap.Builder builder =
                IMessageProto.IMessageWrap.newBuilder();

        /* 1. 基础字段 */
        if (msg.getCode() != null) builder.setCode(msg.getCode());
        if (msg.getToken() != null) builder.setToken(msg.getToken());
        if (msg.getRequestId() != null) builder.setRequestId(msg.getRequestId());
        if (msg.getTimestamp() != null) builder.setTimestamp(msg.getTimestamp());
        if (msg.getClientIp() != null) builder.setClientIp(msg.getClientIp());
        if (msg.getUserAgent() != null) builder.setUserAgent(msg.getUserAgent());
        if (msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
            builder.putAllMetadata(msg.getMetadata());
        }

        /* 2. data -> Any（万能处理） */
        Object data = msg.getData();
        if (data != null) {
            builder.setData(ProtoJsonUtils.packAny(data));
        }

        byte[] bytes = builder.build().toByteArray();
        out.add(Unpooled.wrappedBuffer(bytes));
    }
}
