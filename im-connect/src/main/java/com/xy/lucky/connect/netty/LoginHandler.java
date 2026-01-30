package com.xy.lucky.connect.netty;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.netty.process.impl.LoginProcess;
import com.xy.lucky.connect.netty.service.ChannelCleanupHelper;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录消息处理器
 * <p>
 * - 处理 REGISTER 类型的登录消息
 * - 连接断开时清理用户资源
 */
@Slf4j(topic = LogConstant.Login)
@Component
@ChannelHandler.Sharable
public class LoginHandler extends SimpleChannelInboundHandler<IMessageWrap<Object>> {

    @Autowired
    private LoginProcess loginProcess;

    @Autowired
    private ChannelCleanupHelper cleanupHelper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMessageWrap<Object> message) {
        int code = message.getCode();

        if (code == IMessageType.REGISTER.getCode()) {
            try {
                loginProcess.process(ctx, message);
            } catch (Throwable t) {
                log.error("登录处理异常: channelId={}, error={}", ctx.channel().id().asShortText(), t.getMessage(), t);
            }
        } else {
            // 非登录消息，不处理也不传递 (登录完成后此 Handler 不应再收到其他消息)
            log.debug("收到非登录消息，忽略: code={}, channelId={}", code, ctx.channel().id().asShortText());
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        String userId = cleanupHelper.getUserId(ctx);

        if (userId == null) {
            log.debug("handlerRemoved: 无绑定用户, channelId={}", ctx.channel().id().asShortText());
            return;
        }

        // 统一清理资源 (内存映射 + Redis 路由)
        cleanupHelper.cleanup(ctx, "handlerRemoved", false);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String userId = cleanupHelper.getUserId(ctx);
        log.error("业务通信异常: userId={}, channelId={}, error={}", userId, ctx.channel().id().asShortText(), cause.getMessage());

        // 异常时清理资源并关闭连接
        cleanupHelper.cleanup(ctx, "exceptionCaught", true);
    }
}
