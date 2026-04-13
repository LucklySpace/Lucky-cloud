package com.xy.lucky.connect.netty.service.websocket.codec.json;

import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.core.model.IMessageWrap;
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
        if (msg instanceof TextWebSocketFrame frame) {
            try {
                IMessageWrap pojo = JacksonUtil.parseObject(frame.text(), IMessageWrap.class);
                if (pojo != null) {
                    ctx.fireChannelRead(pojo);
                } else {
                    log.warn("JSON 解码结果为空");
                }
            } catch (Exception e) {
                log.error("JSON 解码失败", e);
            } finally {
                frame.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
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
                String json = JacksonUtil.toJSONString(imMsg);
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
