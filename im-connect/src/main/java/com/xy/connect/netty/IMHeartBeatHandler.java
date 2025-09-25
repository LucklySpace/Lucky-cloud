package com.xy.connect.netty;


import com.xy.connect.channel.UserChannelMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.process.impl.HeartBeatProcess;
import com.xy.connect.redis.RedisTemplate;
import com.xy.core.constants.IMConstant;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMConnectMessage;
import com.xy.core.utils.StringUtils;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.Netty)
@Component
@ChannelHandler.Sharable
public class IMHeartBeatHandler extends SimpleChannelInboundHandler<IMConnectMessage<Object>> {

    private static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf(IMConstant.IM_USER);

    @Autowired
    private HeartBeatProcess heartBeatProcess;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserChannelMap userChannelMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMConnectMessage<Object> message) {
        int code = message.getCode();
        if (code == IMessageType.HEART_BEAT.getCode()) {
            try {
                heartBeatProcess.process(ctx, message);
                // 消费消息，不向下传递
            } catch (Throwable t) {
                log.error("心跳处理异常, channelId={}", ctx.channel().id().asLongText(), t);
            }
        } else {
            // 非心跳，向下传递给下一个 Handler
            ctx.fireChannelRead(message);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent ise && ise.state() == IdleState.ALL_IDLE) {
            handleIdleState(ctx);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void handleIdleState(ChannelHandlerContext ctx) throws Exception {
        final String channelId = ctx.channel().id().asLongText();

        if (log.isDebugEnabled()) {
            log.debug("心跳检查 ALL_IDLE channelId={}", channelId);
        }

        String userId = safeGetUserId(ctx);
        if (!StringUtils.hasText(userId)) {
            if (log.isWarnEnabled()) {
                log.warn("心跳超时但未绑定 userId, channel={}", channelId);
            }
            try {
                ctx.close();
            } catch (Throwable ignored) {
            }
            return;
        }

        userChannelMap.removeByChannel(ctx.channel());

        asyncRedisDelete(IMConstant.USER_CACHE_PREFIX + userId);

        try {
            ctx.close();
        } catch (Throwable ignored) {
        }

        if (log.isWarnEnabled()) {
            log.warn("心跳超时，断开连接 userId={}, channel={}", userId, channelId);
        }
    }

    private String safeGetUserId(ChannelHandlerContext ctx) {
        if (ctx == null) return null;
        return ctx.channel().attr(USER_ID_ATTR_KEY).get();
    }

    private void asyncRedisDelete(final String key) throws Exception {
        if (key == null || key.isEmpty()) return;
        redisTemplate.del(key);
    }
}