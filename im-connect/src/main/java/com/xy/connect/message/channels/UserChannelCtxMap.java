package com.xy.connect.message.channels;

import com.xy.connect.netty.IMChannelHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;


public class UserChannelCtxMap {

    // 全局的通道组，用于存放所有活跃的通道，线程安全
    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 定义一个 AttributeKey，用于在每个 Channel 的属性中存放对应的 userId
    private static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf("userId");

    /**
     * 将一个用户的通道添加到全局通道组中，并绑定对应的 userId 属性。
     * 同时为该通道添加关闭监听器，通道关闭时自动触发资源清理。
     *
     * @param userId 用户标识
     * @param ctx    通道处理上下文
     */
    public static void addChannel(String userId, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        // 设置通道属性，将 userId 存储到 Channel 的 AttributeMap 中
        channel.attr(USER_ID_ATTR_KEY).set(userId);
        // 将该通道添加到全局通道组中，便于统一管理
        CHANNEL_GROUP.add(channel);
        // 添加通道关闭监听器，通道关闭时自动清理关联的 userId 通道
        channel.closeFuture().addListener(future -> {
            // 优化：先获取属性值，避免重复调用
            String uid = channel.attr(USER_ID_ATTR_KEY).get();
            if (uid != null) {
                removeChannel(uid);
            }
        });
    }

    /**
     * 根据指定的 userId 移除所有相关通道。
     * 遍历全局通道组，找到所有绑定了相同 userId 的通道，并逐一关闭它们。
     *
     * @param userId 用户标识
     */
    public static void removeChannel(String userId) {
        // 使用 Stream API 遍历所有通道，筛选出具有匹配 userId 的通道后关闭
        CHANNEL_GROUP.stream()
                .filter(channel -> {
                    String uid = channel.attr(USER_ID_ATTR_KEY).get();
                    return uid != null && uid.equals(userId);
                })
                .forEach(Channel::close);
    }

    /**
     * 根据指定的 userId 获取第一个匹配的通道对应的 ChannelHandlerContext。
     * 遍历全局通道组，找到第一个绑定了该 userId 的通道，
     * 然后从该通道的 ChannelPipeline 中获取 IMChannelHandler 对应的上下文。
     *
     * @param userId 用户标识
     * @return 与 userId 匹配的通道对应的 ChannelHandlerContext，若不存在则返回 null
     */
    public static ChannelHandlerContext getChannel(String userId) {
        return CHANNEL_GROUP.stream()
                .filter(channel -> {
                    String uid = channel.attr(USER_ID_ATTR_KEY).get();
                    return uid != null && uid.equals(userId);
                })
                .findFirst()
                // 从找到的通道中获取其 ChannelPipeline，然后通过 pipeline 获取 IMChannelHandler 的上下文
                .map(Channel::pipeline)
                .map(pipeline -> pipeline.context(IMChannelHandler.class))
                .orElse(null);
    }

    /**
     * 获取所有活跃通道的集合。
     *
     * @return 当前全局的 ChannelGroup，包含所有活跃通道
     */
    public static ChannelGroup getAllChannels() {
        return CHANNEL_GROUP;
    }

    /**
     * 校验给定的 ChannelHandlerContext 是否与指定的 userId 匹配，
     * 同时该通道必须存在于全局通道组中。
     *
     * @param userId 用户标识
     * @param ctx    待校验的通道处理上下文
     * @return 如果通道包含 userId 属性且与给定 userId 匹配，同时该通道在全局通道组中，则返回 true；否则返回 false
     */
    public static boolean isValidChannel(String userId, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        String uid = channel.attr(USER_ID_ATTR_KEY).get();
        return uid != null && uid.equals(userId) && CHANNEL_GROUP.contains(channel);
    }
}
