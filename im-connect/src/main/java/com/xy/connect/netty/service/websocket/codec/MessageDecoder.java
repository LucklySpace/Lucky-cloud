package com.xy.connect.netty.service.websocket.codec;


import com.xy.connect.utils.JsonUtil;
import com.xy.imcore.model.IMWsConnMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class MessageDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          TextWebSocketFrame textWebSocketFrame,
                          List<Object> list) throws Exception {
        list.add(JsonUtil.parseObject(textWebSocketFrame.text(), IMWsConnMessage.class));
    }
}
