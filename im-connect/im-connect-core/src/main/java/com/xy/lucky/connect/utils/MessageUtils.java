package com.xy.lucky.connect.utils;

import com.xy.lucky.core.model.IMessageWrap;
import io.netty.channel.ChannelHandlerContext;

public class MessageUtils {

    public static boolean sendError(ChannelHandlerContext ctx, Integer code, String errorInfo) {
        return send(ctx, IMessageWrap.builder().code(code).data(errorInfo).build());
    }

    public static boolean send(ChannelHandlerContext ctx, IMessageWrap msg) {
        if (ctx == null || msg == null || !ctx.channel().isOpen()) {
            return false;
        }
        ctx.channel().writeAndFlush(msg);
        return true;
    }

    public static void close(ChannelHandlerContext ctx) {
        ctx.close();
    }

}
