package com.xy.connect.netty.service.websocket.codec.proto;

import com.xy.connect.domain.proto.ImConnectProto;
import com.xy.connect.utils.ProtoJsonUtils;
import com.xy.core.model.IMConnectMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ProtobufMessageDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          BinaryWebSocketFrame frame,
                          List<Object> out) throws Exception {

        ImConnectProto.IMConnectMessage proto =
                ImConnectProto.IMConnectMessage.parseFrom(frame.content().nioBuffer());

        IMConnectMessage<Object> pojo = new IMConnectMessage<>();
        pojo.setCode(proto.getCode());
        pojo.setToken(proto.getToken());
        pojo.setRequestId(proto.getRequestId());
        pojo.setTimestamp(proto.getTimestamp());
        pojo.setClientIp(proto.getClientIp());
        pojo.setUserAgent(proto.getUserAgent());
        if (!proto.getMetadataMap().isEmpty()) {
            pojo.setMetadata(proto.getMetadataMap());
        }

        // 只处理 Any（有就解包）
        if (proto.hasData()) {
            Object unpacked = ProtoJsonUtils.unpackAny(proto.getData());
            pojo.setData(unpacked);
        }

        out.add(pojo);
    }

}