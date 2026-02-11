package com.xy.lucky.connect.netty.service.tcp.codec.proto;

import com.xy.lucky.connect.domain.proto.IMessageProto;
import com.xy.lucky.connect.utils.ProtoJsonUtils;
import com.xy.lucky.core.model.IMessageWrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 * 自定义 Protobuf 解码器
 */
@Slf4j
public class ProtobufMessageDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          BinaryWebSocketFrame frame,
                          List<Object> out) throws Exception {

        // 从 frame 中取得完整字节数组
        ByteBuf content = frame.content();
        int len = content.readableBytes();
        byte[] bytes = new byte[len];
        content.getBytes(content.readerIndex(), bytes);

        // 解析 proto IMessageWrap
        IMessageProto.IMessageWrap proto;
        try {
            proto = IMessageProto.IMessageWrap.parseFrom(bytes);
        } catch (Exception e) {
            log.warn("Failed to parse ImConnectProto.IMMessage: {}", e.getMessage());
            // 解析失败：可选择记录并丢弃/抛出/关闭连接。这里丢弃该帧以保持健壮性
            return;
        }

        // 映射为 POJO
        IMessageWrap<Object> pojo = new IMessageWrap<>();
        // 基础字段直接映射
        pojo.setCode(proto.getCode());
        pojo.setToken(proto.getToken());
        pojo.setRequestId(proto.getRequestId());
        pojo.setTimestamp(proto.getTimestamp());
        pojo.setClientIp(proto.getClientIp());
        pojo.setUserAgent(proto.getUserAgent());
        if (!proto.getMetadataMap().isEmpty()) {
            pojo.setMetadata(proto.getMetadataMap());
        }

        // 处理 data (Any) 字段 —— 在本类内完成所有解析逻辑
        if (proto.hasData()) {
            Object unpacked = ProtoJsonUtils.unpackAny(proto.getData());
            pojo.setData(unpacked);
        }

        out.add(pojo);
    }
}
