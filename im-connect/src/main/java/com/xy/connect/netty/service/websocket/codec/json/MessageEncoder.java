package com.xy.connect.netty.service.websocket.codec.json;


import com.xy.connect.utils.JacksonUtil;
import com.xy.imcore.model.IMConnectMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class MessageEncoder extends MessageToMessageEncoder<IMConnectMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, IMConnectMessage message,
                          List<Object> list) throws Exception {
        list.add(new TextWebSocketFrame(JacksonUtil.toJson(message)));
    }
}
