package com.xy.connect.netty.service.websocket.codec;


import com.xy.connect.utils.JsonUtil;
import com.xy.imcore.model.IMWsConnMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class MessageEncoder extends MessageToMessageEncoder<IMWsConnMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, IMWsConnMessage message,
                          List<Object> list) throws Exception {
        list.add(new TextWebSocketFrame(JsonUtil.toJSONString(message)));
    }
}
