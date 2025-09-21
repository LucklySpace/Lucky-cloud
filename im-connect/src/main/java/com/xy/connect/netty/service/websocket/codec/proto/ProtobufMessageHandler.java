package com.xy.connect.netty.service.websocket.codec.proto;

import com.xy.connect.domain.proto.ImConnectProto;
import com.xy.connect.utils.ProtoJsonUtils;
import com.xy.core.model.IMConnectMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ChannelHandler.Sharable
public class ProtobufMessageHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
            try {
                ImConnectProto.IMConnectMessage proto =
                        ImConnectProto.IMConnectMessage.parseFrom(frame.content().nioBuffer());

                IMConnectMessage<Object> pojo = new IMConnectMessage<>();
                pojo.setCode(proto.getCode());
                pojo.setToken(proto.getToken());
                pojo.setRequestId(proto.getRequestId());
                pojo.setTimestamp(proto.getTimestamp());
                pojo.setClientIp(proto.getClientIp());
                pojo.setUserAgent(proto.getUserAgent());
                pojo.setMessage(proto.getMessage());
                pojo.setDeviceName(proto.getDeviceName());
                pojo.setDeviceType(proto.getDeviceType());
                if (!proto.getMetadataMap().isEmpty()) {
                    pojo.setMetadata(proto.getMetadataMap());
                }

                // 只处理 Any（有就解包）
                if (proto.hasData()) {
                    Object unpacked = ProtoJsonUtils.unpackAny(proto.getData());
                    pojo.setData(unpacked);
                }

                // 替换消息为 POJO 并 forward
                ctx.fireChannelRead(pojo);
            } catch (Exception e) {
                log.error("Protobuf 解码失败", e);
                ctx.fireChannelRead(msg); // 或丢弃/关闭，根据业务
            }
        } else {
            // 非 BinaryWebSocketFrame，直接 forward
            ctx.fireChannelRead(msg);
        }
        // 释放帧资源
        if (msg instanceof BinaryWebSocketFrame) {
            ((BinaryWebSocketFrame) msg).release();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof IMConnectMessage) {
            IMConnectMessage imMsg = (IMConnectMessage) msg;
            try {
                if (imMsg == null) {
                    ctx.write(msg, promise);
                    return;
                }

                ImConnectProto.IMConnectMessage.Builder builder =
                        ImConnectProto.IMConnectMessage.newBuilder();

                /* 1. 基础字段 */
                if (imMsg.getCode() != null) builder.setCode(imMsg.getCode());
                if (imMsg.getToken() != null) builder.setToken(imMsg.getToken());
                if (imMsg.getRequestId() != null) builder.setRequestId(imMsg.getRequestId());
                if (imMsg.getTimestamp() != null) builder.setTimestamp(imMsg.getTimestamp());
                if (imMsg.getClientIp() != null) builder.setClientIp(imMsg.getClientIp());
                if (imMsg.getUserAgent() != null) builder.setUserAgent(imMsg.getUserAgent());
                if (imMsg.getDeviceName() != null) builder.setDeviceName(imMsg.getDeviceName());
                if (imMsg.getDeviceType() != null) builder.setDeviceType(imMsg.getDeviceType());
                if (imMsg.getMessage() != null) builder.setMessage(imMsg.getMessage());
                if (imMsg.getMetadata() != null && !imMsg.getMetadata().isEmpty()) {
                    builder.putAllMetadata(imMsg.getMetadata());
                }

                /* 2. data -> Any（万能处理） */
                Object data = imMsg.getData();
                if (data != null) {
                    builder.setData(ProtoJsonUtils.packAny(data));
                }

                /* 3. 输出二进制帧 */
                byte[] bytes = builder.build().toByteArray();
                BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes));
                ctx.write(frame, promise);
            } catch (Exception e) {
                log.error("Protobuf 编码失败", e);
                ctx.write(msg, promise); // 或丢弃，根据业务
            }
        } else {
            // 非 IMConnectMessage，直接 forward
            ctx.write(msg, promise);
        }
    }
}