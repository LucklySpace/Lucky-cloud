package com.xy.lucky.connect.netty;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.netty.process.impl.HeartBeatProcess;
import com.xy.lucky.connect.netty.service.ChannelCleanupHelper;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳消息处理器
 * <p>
 * - 处理 HEART_BEAT 类型的心跳消息
 * - 处理空闲超时事件，清理超时连接
 * - 非心跳消息传递给下一个 Handler
 */
@Slf4j(topic = LogConstant.HeartBeat)
@Component
@ChannelHandler.Sharable
public class HeartBeatHandler extends SimpleChannelInboundHandler<IMessageWrap<Object>> {

    @Autowired
    private HeartBeatProcess heartBeatProcess;

    @Autowired
    private ChannelCleanupHelper cleanupHelper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMessageWrap<Object> message) {
        int code = message.getCode();

        if (code == IMessageType.HEART_BEAT.getCode()) {
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

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent ise && ise.state() == IdleState.ALL_IDLE) {
            handleIdleTimeout(ctx);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 处理空闲超时
     */
    private void handleIdleTimeout(ChannelHandlerContext ctx) {
        String userId = cleanupHelper.getUserId(ctx);
        String channelId = ctx.channel().id().asShortText();

        if (userId == null) {
            log.warn("心跳超时但无绑定用户, channelId={}", channelId);
            ctx.close();
            return;
        }

        log.warn("心跳超时，断开连接: userId={}, channelId={}", userId, channelId);

        // 统一清理资源并关闭连接
        cleanupHelper.cleanup(ctx, "heartbeat_timeout", true);
    }
}
