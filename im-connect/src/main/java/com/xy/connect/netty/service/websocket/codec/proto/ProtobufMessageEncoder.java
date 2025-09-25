package com.xy.connect.netty.service.websocket.codec.proto;


import com.google.protobuf.util.JsonFormat;
import com.xy.connect.domain.proto.IMessageProto;
import com.xy.connect.utils.ProtoJsonUtils;
import com.xy.core.model.IMessageWrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ProtobufMessageEncoder extends MessageToMessageEncoder<IMessageWrap> {

    private static final JsonFormat.Parser PROTO_JSON_PARSER =
            JsonFormat.parser().ignoringUnknownFields();

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          IMessageWrap msg,
                          List<Object> out) throws Exception {

        if (msg == null) return;

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

        /* 3. 输出二进制帧 */
        byte[] bytes = builder.build().toByteArray();
        out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes)));
    }

}