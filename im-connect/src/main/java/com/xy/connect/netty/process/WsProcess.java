package com.xy.connect.netty.process;

import cn.hutool.core.util.StrUtil;
import com.xy.connect.utils.MessageUtils;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMWsConnMessage;
import com.xy.imcore.utils.JwtUtil;
import io.netty.channel.ChannelHandlerContext;

public interface WsProcess {

    void process(ChannelHandlerContext ctx, IMWsConnMessage sendInfo);


    default String parseUsername(ChannelHandlerContext ctx, String token) {
        if (StrUtil.isEmpty(token)) {
            MessageUtils.sendError(ctx, IMessageType.ERROR.getCode(), "未登录");
            throw new IllegalArgumentException("未登录");
        }


        if (!JwtUtil.validate(token)) {
            MessageUtils.sendError(ctx, IMessageType.LOGIN_OVER.getCode(), "token已失效");
            throw new IllegalArgumentException("token已失效");
        }

        try {
            return JwtUtil.getUsername(token);
        } catch (Exception e) {
            MessageUtils.sendError(ctx, IMessageType.ERROR.getCode(), "token有误");
            throw new IllegalArgumentException("token有误");
        }
    }

}
