package com.xy.connect.netty.service.websocket.codec.json;

import com.xy.connect.utils.JacksonUtil;
import com.xy.core.model.IMessageWrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class JsonMessageHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            try {
                IMessageWrap pojo = JacksonUtil.fromJson(frame.text(), IMessageWrap.class);
                // 替换消息为 POJO 并 forward
                ctx.fireChannelRead(pojo);
            } catch (Exception e) {
                log.error("JSON 解码失败", e);
                ctx.fireChannelRead(msg); // 或丢弃/关闭，根据业务
            }
        } else {
            // 非 TextWebSocketFrame，直接 forward
            ctx.fireChannelRead(msg);
        }
        // 释放帧资源
        ((TextWebSocketFrame) msg).release();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof IMessageWrap) {
            IMessageWrap imMsg = (IMessageWrap) msg;
            try {
                if (imMsg == null) {
                    ctx.write(msg, promise);
                    return;
                }
                String json = JacksonUtil.toJson(imMsg);
                TextWebSocketFrame frame = new TextWebSocketFrame(json);
                ctx.write(frame, promise);
            } catch (Exception e) {
                log.error("JSON 编码失败", e);
                ctx.write(msg, promise); // 或丢弃，根据业务
            }
        } else {
            // 非 IMConnectMessage，直接 forward
            ctx.write(msg, promise);
        }
    }
}