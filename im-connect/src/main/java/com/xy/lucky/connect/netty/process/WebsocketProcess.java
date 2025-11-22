package com.xy.lucky.connect.netty.process;

import cn.hutool.core.util.StrUtil;
import com.xy.lucky.connect.utils.MessageUtils;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.JwtUtil;
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

        // 判断 token 是否为空
        if (StrUtil.isEmpty(token)) {
            MessageUtils.sendError(ctx, IMessageType.ERROR.getCode(), "未登录");
            throw new IllegalArgumentException("未登录");
        }

        // 检验token是否过期
        if (!JwtUtil.validate(token)) {
            MessageUtils.sendError(ctx, IMessageType.LOGIN_OVER.getCode(), "token已失效");
            throw new IllegalArgumentException("token已失效");
        }

        try {
            // 从 token 中获取 userId
            return JwtUtil.getUsername(token);
        } catch (Exception e) {
            MessageUtils.sendError(ctx, IMessageType.ERROR.getCode(), "token有误");
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
        // 获取剩余时间
        return JwtUtil.getRemaining(token, TimeUnit.MINUTES);
    }

}
