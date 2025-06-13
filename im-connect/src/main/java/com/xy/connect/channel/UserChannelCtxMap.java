package com.xy.connect.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * 用户与 Channel 的映射管理类。
 * 支持一个用户绑定一个 Channel，不支持多端设备连接（如 Web、Android、iOS 等同时在线）。
 * 若需支持多端，请使用嵌套 Map 或更复杂结构。
 */
public class UserChannelCtxMap {

//    /**
//     * Netty 全局 Channel 组，用于统一管理所有活跃连接。
//     * 注意：该结构会占用大量内存，且在大规模连接时（百万级）遍历较慢，可考虑移除。
//     */
//    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 通道属性键，用于将 userId 附加到 Channel 上，便于后续查找。
     */
    private static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf("userId");

    /**
     * 用户 ID 与其 Channel 映射表。
     * 仅支持一个用户 ID 对应一个连接，不能用于多端并发登录。
     * 如需支持多个终端，需替换为 Map<String, Map<String, Channel>>。
     */
    private static final ConcurrentMap<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 绑定用户 ID 与通道，并添加到全局管理中。
     * 若该用户已存在旧连接，将被新连接覆盖。
     * 添加关闭监听器，连接关闭时自动清除资源。
     *
     * @param userId 用户 ID
     * @param ctx    通道处理上下文
     */
    public static void addChannel(String userId, ChannelHandlerContext ctx) {

        Channel channel = ctx.channel();

        // 设置属性值，用于后续校验或清理
        channel.attr(USER_ID_ATTR_KEY).set(userId);

        // 绑定用户 ID 到 Channel
        USER_CHANNEL_MAP.put(userId, channel);

        // 监听连接关闭事件，自动清除映射
        channel.closeFuture().addListener(future -> removeChannel(userId));

    }

    /**
     * 移除指定用户的通道映射。
     * 包括从全局组中移除，并关闭连接。
     *
     * @param userId 用户 ID
     */
    public static void removeChannel(String userId) {

        Channel channel = USER_CHANNEL_MAP.remove(userId);

        if (channel != null) {
            // 可选操作，防止连接未被关闭
            channel.close();
        }
    }

    /**
     * 根据用户 ID 获取其 ChannelHandlerContext。
     * 用于发送消息或操作特定通道。
     *
     * @param userId 用户 ID
     * @return 对应的 ChannelHandlerContext，若不存在或非活跃则返回 null
     */
    public static Channel getChannel(String userId) {
        if (userId == null) {
            return null;
        }
        return USER_CHANNEL_MAP.get(userId);
    }

    /**
     * 获取当前所有活跃通道的集合。
     *
     * @return ChannelGroup 包含所有 Channel
     */
    public static ConcurrentMap<String, Channel> getAllChannels() {
        return USER_CHANNEL_MAP;
    }

    /**
     * 校验通道是否有效。
     * 用于防止非预期连接使用 Channel。
     *
     * @param userId 用户 ID
     * @param ctx    上下文
     * @return 是否为有效连接
     */
    public static boolean isValidChannel(String userId, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        return channel.equals(USER_CHANNEL_MAP.get(userId)) && channel.isActive();
    }
}
