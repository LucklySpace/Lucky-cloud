package com.xy.lucky.connect.netty.service.websocket.codec.json;


import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.core.model.IMessageWrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class JsonMessageDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          TextWebSocketFrame textWebSocketFrame,
                          List<Object> list) throws Exception {
        list.add(JacksonUtil.parseObject(textWebSocketFrame.text(), IMessageWrap.class));
    }
}
