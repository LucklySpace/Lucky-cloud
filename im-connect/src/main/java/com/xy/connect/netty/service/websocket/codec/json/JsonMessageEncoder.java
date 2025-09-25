package com.xy.connect.netty.service.websocket.codec.json;


import com.xy.connect.utils.JacksonUtil;
import com.xy.core.model.IMessageWrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class JsonMessageEncoder extends MessageToMessageEncoder<IMessageWrap> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, IMessageWrap message,
                          List<Object> list) throws Exception {
        list.add(new TextWebSocketFrame(JacksonUtil.toJson(message)));
    }
}
