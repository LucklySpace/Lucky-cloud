package com.xy.lucky.connect.netty.process;


import com.xy.lucky.core.model.IMessageWrap;
import io.netty.channel.ChannelHandlerContext;

/**
 * 抽象处理类
 */
public interface WebsocketProcess {

    void process(ChannelHandlerContext ctx, IMessageWrap sendInfo) throws Exception;

}
