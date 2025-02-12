package com.xy.connect.netty;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.channels.UserChannelCtxMap;
import com.xy.connect.netty.process.HeartBeatProcess;
import com.xy.connect.netty.process.LoginProcess;
import com.xy.connect.netty.process.WsProcess;
import com.xy.connect.utils.JedisUtil;
import com.xy.connect.utils.JsonUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMWsConnMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.xy.imcore.constants.Constant.IMUSERPREFIX;

/**
 * TCP WebSocket connection handler.
 */
@Slf4j(topic = LogConstant.NETTY)
public class IMChannelHandler extends SimpleChannelInboundHandler<IMWsConnMessage> {

    // 常量定义，避免硬编码
    private static final String userId_ATTR_KEY = "userId";

    /**
     * 读取消息后处理
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMWsConnMessage message) throws Exception {
        if (message == null) {
            return;
        }

        Optional.ofNullable(IMessageType.getByCode(message.getCode()))
                .ifPresentOrElse(
                        messageType -> handleMessage(ctx, messageType, message),
                        () -> log.warn("未识别的消息类型: {}", message.getCode())
                );
    }

    /**
     * 根据消息类型处理对应逻辑
     */
    private void handleMessage(ChannelHandlerContext ctx, IMessageType messageType, IMWsConnMessage message) {
        WsProcess wsProcess = switch (messageType) {
            case LOGIN -> new LoginProcess();
            case HEART_BEAT -> new HeartBeatProcess();
            default -> null;
        };

        if (ObjectUtil.isNotEmpty(wsProcess)) {
            wsProcess.process(ctx, message);
        } else {
            log.warn("未处理的消息类型: {}", messageType);
        }
    }

    /**
     * 异常处理，记录异常日志
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通信异常，channelId: {}, 异常信息: {}", ctx.channel().id().asLongText(), cause.getMessage(), cause);
        ctx.close(); // 确保出现异常时关闭连接
    }

    /**
     * 处理新连接
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("建立连接，channelId: {}", ctx.channel().id().asLongText());
    }

    /**
     * 处理连接断开
     * 退出时清除 UserChannelCtxMap 中用户的channel ，清除redis中的用户信息
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        String userId = getUserIdFromChannel(ctx);

        if (isValidChannel(userId, ctx)) {
            // 移除channel并关闭连接
            UserChannelCtxMap.removeChannel(userId);

            // 清除redis中的用户信息
            JedisUtil.getInstance().del(IMUSERPREFIX + userId);

            log.info("断开连接, userId: {}, channel_id: {}", userId, ctx.channel().id().asLongText());
        }
    }

    /**
     * 用户事件触发器，用于处理心跳超时等事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            handleIdleState(ctx, idleEvent);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 处理空闲状态事件
     */
    private void handleIdleState(ChannelHandlerContext ctx, IdleStateEvent idleEvent) {
        IdleState state = idleEvent.state();
        log.info("IdleStateEvent, 时间: {}, 状态: {}", DateUtil.now(), state);

        switch (state) {
            case READER_IDLE -> log.info("读取超时，channelId: {}", ctx.channel().id().asLongText());
            case WRITER_IDLE -> log.info("写入超时，channelId: {}", ctx.channel().id().asLongText());
            case ALL_IDLE -> {
                String userId = getUserIdFromChannel(ctx);
                if (userId != null) {
                    log.warn("心跳超时，断开连接, userId: {}", userId);
                    ctx.close();
                }
            }
        }
    }

    /**
     * 从Channel中获取用户ID
     */
    private String getUserIdFromChannel(ChannelHandlerContext ctx) {
        return (String) ctx.channel().attr(AttributeKey.valueOf(userId_ATTR_KEY)).get();
    }

    /**
     * 验证是否为有效的Channel
     */
    private boolean isValidChannel(String userId, ChannelHandlerContext ctx) {
        ChannelHandlerContext storedCtx = UserChannelCtxMap.getChannel(userId);
        return storedCtx != null && ctx.channel().id().equals(storedCtx.channel().id());
    }
}
