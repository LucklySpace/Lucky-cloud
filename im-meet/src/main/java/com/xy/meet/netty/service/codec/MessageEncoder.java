package com.xy.meet.netty.service.codec;


import com.xy.meet.entity.Message;
import com.xy.utils.json.JacksonUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.List;

public class MessageEncoder extends MessageToMessageEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message message,
                          List<Object> list) throws Exception {
        list.add(new TextWebSocketFrame(JacksonUtils.toJSONString(message)));
    }
}
