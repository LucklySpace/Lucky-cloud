package com.xy.connect.netty;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.xy.connect.channel.UserChannelCtxMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.process.RedisBatchManager;
import com.xy.connect.netty.process.WebsocketProcess;
import com.xy.connect.netty.process.impl.HeartBeatProcess;
import com.xy.connect.netty.process.impl.LoginProcess;
import com.xy.connect.redis.RedisTemplate;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMConnectMessage;
import com.xy.imcore.utils.StringUtils;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.xy.imcore.constants.IMConstant.IM_USER_PREFIX;

/**
 * WebSocket 消息处理器（Netty ChannelHandler）
 * 负责处理连接、断开、异常、心跳、登录等业务逻辑。
 */
@Slf4j(topic = LogConstant.Netty)
@Component
@ChannelHandler.Sharable
public class IMChannelHandler extends SimpleChannelInboundHandler<IMConnectMessage<?>> {

    // 用于绑定在 Channel 上的用户ID属性
    private static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf("userId");

    @Autowired
    private RedisBatchManager redisBatchManager;

    @Autowired
    private LoginProcess loginProcess;

    @Autowired
    private HeartBeatProcess heartBeatProcess;

    /**
     * 接收到 WebSocket 消息时的回调方法
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMConnectMessage message) {
        if (message == null) {
            log.warn("接收到空消息，channelId: {}", ctx.channel().id().asLongText());
            return;
        }

        IMessageType messageType = IMessageType.getByCode(message.getCode());
        if (messageType == null) {
            log.warn("未知消息类型: {}，channelId: {}", message.getCode(), ctx.channel().id().asLongText());
            return;
        }

        WebsocketProcess processor = switch (messageType) {
            case LOGIN -> loginProcess;
            case HEART_BEAT -> heartBeatProcess;
            default -> null;
        };

        if (processor != null) {
            processor.process(ctx, message);
        } else {
            log.warn("未处理的消息类型: {}，channelId: {}", messageType, ctx.channel().id().asLongText());
        }
    }

    /**
     * 连接建立时触发
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {

        log.info("客户端连接建立，channelId: {}", ctx.channel().id().asLongText());
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
            redisBatchManager.onUserDelete(userId);

            ctx.close();

            log.info("断开连接, userId: {}, channel_id: {}", userId, ctx.channel().id().asLongText());
        }
    }


    /**
     * Netty 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通信异常，channelId: {}, 错误信息: {}", ctx.channel().id().asLongText(), cause.getMessage(), cause);
        ctx.close();
    }

    /**
     * 用户事件触发（如 IdleStateEvent）
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
     * 处理心跳超时逻辑（ALL_IDLE）
     */
    private void handleIdleState(ChannelHandlerContext ctx, IdleStateEvent event) {
        IdleState state = event.state();
        log.debug("心跳状态检查，状态: {}, 时间: {}, channelId: {}",
                state, DateUtil.now(), ctx.channel().id().asLongText());

        if (state == IdleState.ALL_IDLE) {
            String userId = getUserIdFromChannel(ctx);

            if (userId != null) {

                UserChannelCtxMap.removeChannel(userId);

                redisBatchManager.onUserDelete(userId);

                ctx.close();

                log.warn("心跳超时，断开连接，userId: {}", userId);

            } else {

                log.warn("心跳超时但未找到 userId，channelId: {}", ctx.channel().id().asLongText());
            }
        }
    }

    /**
     * 从 Channel 中获取用户ID
     */
    private String getUserIdFromChannel(ChannelHandlerContext ctx) {
        if (ctx == null || ctx.channel() == null) return null;
        return ctx.channel().attr(USER_ID_ATTR_KEY).get();
    }

    /**
     * 验证当前 Channel 是否是最新有效的连接
     */
    private boolean isValidChannel(String userId, ChannelHandlerContext ctx) {

        if (!StringUtils.hasText(userId) || ctx == null) return false;

        Channel currentChannel = ctx.channel();

        Channel storedChannel = UserChannelCtxMap.getChannel(userId);

        return storedChannel != null && currentChannel.id().equals(storedChannel.id());
    }
}
