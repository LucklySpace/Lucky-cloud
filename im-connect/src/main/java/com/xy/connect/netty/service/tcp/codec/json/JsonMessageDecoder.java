package com.xy.connect.netty.service.tcp.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.core.model.IMConnectMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 自定义JSON解码器
 */
@Slf4j
public class JsonMessageDecoder extends ByteToMessageDecoder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 在 pipeline 中前面已用 LengthFieldBasedFrameDecoder 拆帧，所以这里 in 是一整帧
        int len = in.readableBytes();
        byte[] bytes = new byte[len];
        in.readBytes(bytes);
        String txt = new String(bytes, StandardCharsets.UTF_8);
        try {
            IMConnectMessage<?> pojo = MAPPER.readValue(txt, IMConnectMessage.class);
            out.add(pojo);
        } catch (Exception ex) {
            // 解析失败，记录日志并丢弃（或按需把原文传递）
            log.warn("JSON decode failed: {}", ex.getMessage());
        }
    }
}