package com.xy.connect.utils;

import com.xy.core.model.IMConnectMessage;
import io.netty.channel.ChannelHandlerContext;

public class MessageUtils {

    public static boolean sendError(ChannelHandlerContext ctx, Integer code, String errorInfo) {
        return send(ctx, IMConnectMessage.builder().code(code).data(errorInfo).build());
    }

    public static boolean send(ChannelHandlerContext ctx, IMConnectMessage msg) {
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
