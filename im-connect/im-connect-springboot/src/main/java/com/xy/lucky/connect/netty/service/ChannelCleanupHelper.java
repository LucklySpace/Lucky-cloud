package com.xy.lucky.connect.netty.service;

import com.xy.lucky.connect.channel.UserChannelMap;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Channel 清理工具
 * 统一处理连接断开时的内存映射与 Redis 状态清理
 */
@Slf4j(topic = LogConstant.Channel)
@Component
public class ChannelCleanupHelper {

    private static final AttributeKey<String> USER_ID_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);
    private static final AttributeKey<String> DEVICE_TYPE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);

    @Value("${brokerId}")
    private String brokerId;

    @Resource
    private UserChannelMap userChannelMap;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 完整清理连接关联的所有资源
     *
     * @param ctx          ChannelHandlerContext
     * @param reason       清理原因 (用于日志)
     * @param closeChannel 是否关闭 Channel
     */
    public void cleanup(ChannelHandlerContext ctx, String reason, boolean closeChannel) {
        if (ctx == null) return;
        cleanup(ctx.channel(), reason, closeChannel);
    }

    /**
     * 完整清理连接关联的所有资源
     *
     * @param channel      Channel
     * @param reason       清理原因 (用于日志)
     * @param closeChannel 是否关闭 Channel
     */
    public void cleanup(Channel channel, String reason, boolean closeChannel) {
        if (channel == null) return;

        String userId = channel.attr(USER_ID_ATTR).get();
        String deviceType = channel.attr(DEVICE_TYPE_ATTR).get();
        String channelId = channel.id().asLongText();

        // 1. 清理本地内存映射
        userChannelMap.removeByChannel(channel);

        // 2. 清理 Redis 全局路由（仅当用户所有设备都离线时）
        if (StringUtils.hasText(userId)) {
            cleanupRedisRouteIfOrphan(userId);
        }

        // 3. 关闭连接
        if (closeChannel && channel.isActive()) {
            channel.close();
        }

        log.info("连接资源已清理: userId={}, device={}, channelId={}, reason={}", userId, deviceType, channelId, reason);
    }

    /**
     * 仅当用户在本 Broker 上没有任何活跃连接时，才清理 Redis 路由
     */
    private void cleanupRedisRouteIfOrphan(String userId) {
        String routeKey = IMConstant.USER_CACHE_PREFIX + userId;

        // 检查用户在本地是否还有活跃连接
        if (!userChannelMap.getChannelsByUser(userId).isEmpty()) {
            log.debug("用户 {} 仍有活跃连接，保留 Redis 路由", userId);
            return;
        }

        // 检查 Redis 中的 brokerId 是否属于当前节点（避免误删其他节点的路由）
        String json = stringRedisTemplate.opsForValue().get(routeKey);
        if (!StringUtils.hasText(json)) {
            return; // 已不存在，无需处理
        }

        // 简单字符串匹配检查 brokerId（比完整反序列化更高效）
        if (StringUtils.hasText(brokerId) && !json.contains(brokerId)) {
            log.debug("Redis 路由属于其他 Broker，跳过清理 userId={}", userId);
            return;
        }

        stringRedisTemplate.delete(routeKey);
        log.debug("已清理 Redis 路由: key={}", routeKey);
    }

    /**
     * 安全获取 Channel 上绑定的用户 ID
     */
    public String getUserId(ChannelHandlerContext ctx) {
        return ctx != null ? ctx.channel().attr(USER_ID_ATTR).get() : null;
    }

    /**
     * 安全获取 Channel 上绑定的设备类型
     */
    public String getDeviceType(ChannelHandlerContext ctx) {
        return ctx != null ? ctx.channel().attr(DEVICE_TYPE_ATTR).get() : null;
    }
}

