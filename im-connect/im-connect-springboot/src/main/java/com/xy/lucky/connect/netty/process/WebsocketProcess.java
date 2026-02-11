package com.xy.lucky.connect.netty.process;

import com.xy.lucky.connect.utils.MessageUtils;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.core.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeUnit;

public interface WebsocketProcess {

    void process(ChannelHandlerContext ctx, IMessageWrap sendInfo) throws Exception;

    /**
     * 检验用户token信息
     *
     * @param ctx
     * @param token
     * @return
     */
    default String parseUsername(ChannelHandlerContext ctx, String token) {
        if (!StringUtils.hasText(token)) {
            MessageUtils.sendError(ctx, IMessageType.NOT_LOGIN.getCode(), "未登录");
            throw new IllegalArgumentException("未登录");
        }

        if (!JwtUtil.validate(token)) {
            MessageUtils.sendError(ctx, IMessageType.LOGIN_EXPIRED.getCode(), "token已失效");
            throw new IllegalArgumentException("token已失效");
        }

        try {
            return JwtUtil.getUsername(token);
        } catch (Exception e) {
            MessageUtils.sendError(ctx, IMessageType.TOKEN_ERROR.getCode(), "token有误");
            throw new IllegalArgumentException("token有误");
        }
    }

    /**
     * 获取token有限期的剩余时间
     *
     * @param token
     * @return
     */
    default long getRemaining(String token) {
        return JwtUtil.getRemaining(token, TimeUnit.MINUTES);
    }

}
