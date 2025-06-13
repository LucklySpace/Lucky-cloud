package com.xy.connect.netty.service.tcp.codec;

import com.xy.connect.utils.JacksonUtil;
import com.xy.imcore.model.IMConnectMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;


public class MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in,
                          List<Object> list) throws Exception {
        // 判断可读字节数是否大于4（JSON对象格式中最短的四个字符为"{}[]"）
        if (in.readableBytes() < 4) {
            return;
        }
        // 标记读取位置
        in.markReaderIndex();
        // 获取第一个字节
        byte firstByte = in.readByte();
        // 如果不是左大括号 '{' 或者左中括号 '[' 就直接返回
        if (firstByte != '{' && firstByte != '[') {
            return;
        }
        // 将第一个字节还原回缓冲区，以便后续处理
        in.resetReaderIndex();
        // 读取缓冲区中的数据，转换为字符串
        String text = in.readCharSequence(in.readableBytes(), CharsetUtil.UTF_8).toString();
        // 将字符串解析为 JSON 对象，并添加到解码结果列表中
        list.add(JacksonUtil.fromJson(text, IMConnectMessage.class));
    }
}
