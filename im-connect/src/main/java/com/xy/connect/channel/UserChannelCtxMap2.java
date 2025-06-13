package com.xy.connect.channel;//package com.xy.connect.channel;
//
//import com.xy.connect.netty.IMChannelHandler;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.group.ChannelGroup;
//import io.netty.channel.group.DefaultChannelGroup;
//import io.netty.util.AttributeKey;
//import io.netty.util.concurrent.GlobalEventExecutor;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.stream.Collectors;
//
//
//@Slf4j
//public class UserChannelCtxMap {
//
//    // userId -> deviceType -> Channel
//    private static final ConcurrentMap<String, ConcurrentMap<String, Channel>> USER_DEVICE_CHANNELS = new ConcurrentHashMap<>();
//
//    private static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("userId");
//    private static final AttributeKey<String> DEVICE_TYPE_KEY = AttributeKey.valueOf("deviceType");
//
//    /**
//     * 添加连接
//     */
//    public static void addChannel(String userId, String deviceType, ChannelHandlerContext ctx) {
//        Channel channel = ctx.channel();
//        channel.attr(USER_ID_KEY).set(userId);
//        channel.attr(DEVICE_TYPE_KEY).set(deviceType);
//
//        // 多设备支持：设备类型作为 key
//        USER_DEVICE_CHANNELS
//                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
//                .put(deviceType, channel);
//
//        // 连接断开后自动清理
//        channel.closeFuture().addListener(future -> removeChannel(userId, deviceType));
//
//        log.info("User [{}] connected on device [{}], channelId={}", userId, deviceType, channel.id());
//    }
//
//    /**
//     * 移除指定 userId + 设备类型 的通道
//     */
//    public static void removeChannel(String userId, String deviceType) {
//        ConcurrentMap<String, Channel> deviceMap = USER_DEVICE_CHANNELS.get(userId);
//        if (deviceMap != null) {
//            Channel removed = deviceMap.remove(deviceType);
//            if (deviceMap.isEmpty()) {
//                USER_DEVICE_CHANNELS.remove(userId);
//            }
//            if (removed != null) {
//                log.info("Removed channel for user [{}] device [{}]", userId, deviceType);
//            }
//        }
//    }
//
//    /**
//     * 移除用户所有设备连接
//     */
//    public static void removeAllChannels(String userId) {
//        ConcurrentMap<String, Channel> deviceMap = USER_DEVICE_CHANNELS.remove(userId);
//        if (deviceMap != null) {
//            for (Channel channel : deviceMap.values()) {
//                channel.close(); // 会触发自动移除
//            }
//        }
//    }
//
//    /**
//     * 获取用户指定设备的连接
//     */
//    public static ChannelHandlerContext getChannel(String userId, String deviceType) {
//        Channel channel = USER_DEVICE_CHANNELS.getOrDefault(userId,null).get(deviceType);
//        if (channel == null || !channel.isActive()) return null;
//        return channel.pipeline().context(IMChannelHandler.class);
//    }
//
//    /**
//     * 获取某个用户所有设备连接
//     */
//    public static Map<String, ChannelHandlerContext> getAllDevices(String userId) {
//        return USER_DEVICE_CHANNELS.getOrDefault(userId,null).entrySet().stream()
//                .filter(e -> e.getValue().isActive())
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> e.getValue().pipeline().context(IMChannelHandler.class)
//                ));
//    }
//
//    /**
//     * 校验 channel 是否为用户某端连接
//     */
//    public static boolean isValidChannel(String userId, String deviceType, ChannelHandlerContext ctx) {
//        Channel channel = ctx.channel();
//        String uid = channel.attr(USER_ID_KEY).get();
//        String dtype = channel.attr(DEVICE_TYPE_KEY).get();
//        return userId.equals(uid)
//                && deviceType.equals(dtype)
//                && USER_DEVICE_CHANNELS
//                .getOrDefault(userId, null)
//                .get(deviceType) == channel;
//    }
//
//    /**
//     * 获取当前在线用户数
//     */
//    public static int getOnlineUserCount() {
//        return USER_DEVICE_CHANNELS.size();
//    }
//
//    /**
//     * 获取总连接数
//     */
//    public static int getTotalConnectionCount() {
//        return USER_DEVICE_CHANNELS.values().stream()
//                .mapToInt(Map::size)
//                .sum();
//    }
//}
