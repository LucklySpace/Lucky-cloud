package com.xy.lucky.connect.netty;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.netty.process.impl.HeartBeatProcess;
import com.xy.lucky.connect.netty.service.HeartbeatTimeoutWheel;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳消息处理器
 * <p>
 * - 处理 HEART_BEAT 类型的心跳消息
 * - 收到消息后续期时间轮任务，避免每连接定时器开销
 * - 非心跳消息传递给下一个 Handler
 */
@Slf4j(topic = LogConstant.HeartBeat)
@Component
@ChannelHandler.Sharable
public class HeartBeatHandler extends SimpleChannelInboundHandler<IMessageWrap<Object>> {

    @Autowired
    private HeartBeatProcess heartBeatProcess;

    @Autowired
    private HeartbeatTimeoutWheel heartbeatTimeoutWheel;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMessageWrap<Object> message) {
        heartbeatTimeoutWheel.renew(ctx.channel());
        int code = message.getCode();

        // 心跳消息
        if (code == IMessageType.HEART_BEAT_PING.getCode()) {
            try {
                heartBeatProcess.process(ctx, message);
                // 心跳消息已处理，不再传递
            } catch (Throwable t) {
                log.error("心跳处理异常: channelId={}, error={}", ctx.channel().id().asShortText(), t.getMessage(), t);
            }
        } else {
            // 非心跳消息，传递给下一个 Handler
            ctx.fireChannelRead(message);
        }
    }
}
