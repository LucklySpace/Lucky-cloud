package com.xy.lucky.connect.netty;


import com.xy.lucky.connect.channel.UserChannelMap;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.netty.process.impl.LoginProcess;
import com.xy.lucky.connect.redis.RedisTemplate;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;


@Slf4j(topic = LogConstant.Login)
@Component
@ChannelHandler.Sharable
public class IMLoginHandler extends SimpleChannelInboundHandler<IMessageWrap<Object>> {

    private static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf(IMConstant.IM_USER);

    @Autowired
    private LoginProcess loginProcess;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserChannelMap userChannelMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMessageWrap<Object> message) {
        int code = message.getCode();
        if (code == IMessageType.LOGIN.getCode()) {
            try {
                loginProcess.process(ctx, message);
            } catch (Throwable t) {
                log.error("登录处理异常, channelId={}", ctx.channel().id().asLongText(), t);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("未处理业务消息 code={}, channel={}", code, ctx.channel().id().asLongText());
            }
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        String userId = safeGetUserId(ctx);

        if (!StringUtils.hasText(userId)) {
            if (log.isDebugEnabled()) {
                log.debug("handlerRemoved: no user bound, channel={}", ctx.channel().id().asLongText());
            }
            return;
        }

        userChannelMap.removeByChannel(ctx.channel());

        asyncRedisDelete(IMConstant.USER_CACHE_PREFIX + userId);

        try {
            Channel ch = ctx.channel();
            if (ch.isActive()) {
                ch.close();
            }
        } catch (Throwable t) {
            log.warn("handlerRemoved: error closing channel {}: {}", ctx.channel().id().asLongText(), t.getMessage());
        }

        if (log.isInfoEnabled()) {
            log.info("断开连接 userId={}, channelId={}", userId, ctx.channel().id().asLongText());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("业务通信异常 channelId={}, cause={}", ctx.channel().id().asLongText(), cause.getMessage(), cause);
        ctx.close();
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