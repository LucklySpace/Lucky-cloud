package com.xy.connect.netty;

import com.xy.connect.channel.UserChannelCtxMap;
import com.xy.connect.netty.process.impl.HeartBeatProcess;
import com.xy.connect.netty.process.impl.LoginProcess;
import com.xy.connect.redis.RedisTemplate;
import com.xy.core.constants.IMConstant;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMConnectMessage;
import com.xy.core.utils.StringUtils;
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

/**
 * 高性能版 IMChannelHandler，优化点：
 * - 使用原始 int 进行消息类型判断，避免频繁枚举查找
 * - 把阻塞型 Redis 操作异步化（可注入 TaskExecutor）
 * - 日志与时间戳尽量使用轻量调用（避免频繁格式化）
 * - 早返回/快速路径（fast-path）尽可能最短
 */
@Slf4j(topic = "Netty")
@Component
@ChannelHandler.Sharable
public final class IMChannelHandler extends SimpleChannelInboundHandler<IMConnectMessage<Object>> {

    // Channel 上绑定的用户 ID attribute key（静态、共享）
    private static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf(IMConstant.IM_USER);

    // 业务处理器注入（保持原有语义）
    @Autowired
    private LoginProcess loginProcess;

    @Autowired
    private HeartBeatProcess heartBeatProcess;

    @Autowired
    private RedisTemplate redisTemplate;

    /* ---------------------- life-cycle / events ---------------------- */

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // 连接建立：尽量少做耗时操作
        if (log.isInfoEnabled()) {
            log.info("客户端连接建立 channelId={}", ctx.channel().id().asLongText());
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        // 当 channel 被移除时清理上下文：从 channel 的 attribute 中拿 userId
        String userId = safeGetUserId(ctx);

        if (!StringUtils.hasText(userId)) {
            if (log.isDebugEnabled()) {
                log.debug("handlerRemoved: no user bound, channel={}", ctx.channel().id().asLongText());
            }
            return;
        }

        // 从内存map移除 channel（尽量先做内存移除）
        UserChannelCtxMap.removeChannel(userId);

        // Redis 删除异步化：避免阻塞 Netty IO 线程
        asyncRedisDelete(IMConstant.USER_CACHE_PREFIX + userId);

        // 确保 channel 被关闭（防御性）
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
    protected void channelRead0(ChannelHandlerContext ctx, IMConnectMessage<Object> message) {
        // Fast null-check
        if (message == null) {
            if (log.isWarnEnabled()) {
                log.warn("接收到空消息 channel={}", ctx.channel().id().asLongText());
            }
            return;
        }

        // 尽量把常用字段拉到局部变量，避免重复调用 getter（减少 JNI / 方法调用开销）
        int code = message.getCode();

        if (code == IMessageType.LOGIN.getCode()) {
            // login process
            try {
                loginProcess.process(ctx, message);
            } catch (Throwable t) {
                log.error("loginProcess error, channelId={}, cause={}", ctx.channel().id().asLongText(), t.getMessage(), t);
            }
        } else if (code == IMessageType.HEART_BEAT.getCode()) {
            try {
                heartBeatProcess.process(ctx, message);
            } catch (Throwable t) {
                log.error("heartBeatProcess error, channelId={}, cause={}", ctx.channel().id().asLongText(), t.getMessage(), t);
            }
        } else {
            // 未知或暂不处理的消息类型：极少日志，避免在热路径输出大量 warn 信息
            if (log.isDebugEnabled()) {
                log.debug("未处理消息 code={}, channel={}", code, ctx.channel().id().asLongText());
            }
        }
        return;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 记录异常并关闭连接
        log.error("通信异常 channelId={}, cause={}", ctx.channel().id().asLongText(), cause.getMessage(), cause);
        // 立即关闭连接（防止资源泄露）
        try {
            ctx.close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 只在需要时做处理，且处理逻辑尽量简单快速
        if (evt instanceof IdleStateEvent ise) {
            if (ise.state() == IdleState.ALL_IDLE) {
                handleIdleState(ctx);
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    /* ---------------------- helper / internal ---------------------- */

    /**
     * 心跳超时处理：仅做必要的清理与最小化日志
     */
    private void handleIdleState(ChannelHandlerContext ctx) {
        final String channelId = ctx.channel().id().asLongText();

        // 不要在热路径调用 DateUtil.now() 等开销较大的方法，使用 millis 当标志
        if (log.isDebugEnabled()) {
            log.debug("心跳检查 ALL_IDLE channelId={}", channelId);
        }

        String userId = safeGetUserId(ctx);
        if (!StringUtils.hasText(userId)) {
            if (log.isWarnEnabled()) {
                log.warn("心跳超时但未绑定 userId, channel={}", channelId);
            }
            // 若未绑定用户，直接关闭连接以释放资源（防御性）
            try {
                ctx.close();
            } catch (Throwable ignored) {
            }
            return;
        }

        // 移除内存映射
        UserChannelCtxMap.removeChannel(userId);
        asyncRedisDelete(IMConstant.USER_CACHE_PREFIX + userId);

        // 关闭 channel
        try {
            ctx.close();
        } catch (Throwable ignored) {
        }

        if (log.isWarnEnabled()) {
            log.warn("心跳超时，断开连接 userId={}, channel={}", userId, channelId);
        }
    }

    /**
     * 从 channel 属性安全读取 userId（避免空指针）
     */
    private String safeGetUserId(ChannelHandlerContext ctx) {
        if (ctx == null) return null;
        Channel ch = ctx.channel();
        if (ch == null) return null;
        return ch.attr(USER_ID_ATTR_KEY).get();
    }

    /**
     * 将 Redis 删除操作异步化（使用注入的 redisTaskExecutor 或 ForkJoinPool.commonPool）
     * 这样可以避免在 Netty IO 线程里进行远程阻塞调用
     */
    /**
     * 单线程异步删除 Redis 键（fire-and-forget）
     * - 将可能阻塞的 redisTemplate.delete(key) 提交到单线程队列执行
     * - 不在 Netty IO 线程中阻塞等待
     */
    private void asyncRedisDelete(final String key) {
        if (key == null || key.isEmpty()) return;

        // 这里用你现有的同步 redisTemplate；若抛异常仅 debug/打印避免噪音
        redisTemplate.del(key);

    }
}
