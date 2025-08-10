package com.xy.connect.netty.service.tcp.codec;


import com.xy.connect.utils.JacksonUtil;
import com.xy.core.model.IMConnectMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


public class MessageEncoder extends MessageToByteEncoder<IMConnectMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, IMConnectMessage message,
                          ByteBuf byteBuf) throws Exception {
        String text = JacksonUtil.toJson(message);

        byte[] bytes = text.getBytes("UTF-8");
        // 写入长度
        byteBuf.writeLong(bytes.length);
        // 写入命令体
        byteBuf.writeBytes(bytes);
    }

}
