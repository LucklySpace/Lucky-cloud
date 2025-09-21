package com.xy.connect.channel;

import com.xy.connect.config.LogConstant;
import com.xy.core.constants.IMConstant;
import com.xy.spring.annotations.core.Component;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户与 Channel 的映射管理类（支持单端/多端）。
 * - 默认单端（deviceType = "default"）。
 * - 若启用多端（配置 netty.multi-device.enabled=true），一个用户可绑定多个设备（如 "desktop"、"web"、"mobile"）。
 * - 通过 deviceType 参数区分，支持无缝切换。
 * <p>
 * 数据结构：
 * userChannels: userId -> ( deviceType -> Channel )
 * channelIndex: channelId -> UserDevice (用于通过 channel 反向查找 userId/deviceType)
 * <p>
 * 语义：
 * - addChannel 会把新的 Channel 绑定到 user+device；若存在旧 Channel 会被关闭（“踢掉旧连接”）
 * - removeChannel 支持按 user+device 或直接按 Channel 移除
 * - 会在 Channel 的 closeFuture 上注册清理回调，确保映射不会泄露
 */
@Slf4j(topic = LogConstant.Channel)
@Component
public class UserChannelMap {

    // channel attribute keys for convenience
    private static final AttributeKey<String> USER_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);

    private static final AttributeKey<String> DEVICE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);

    // userId -> ( deviceType -> Channel )
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Channel>> userChannels = new ConcurrentHashMap<>();

    // channelId(asLongText) -> (userId, deviceType) 反向索引，便于通过 channel 清理
    private final ConcurrentHashMap<String, UserDevice> channelIndex = new ConcurrentHashMap<>();

    /**
     * 把 Channel 与 userId/deviceType 绑定（若该 user+device 已有旧连接，则踢掉旧连接）
     *
     * @param userId     用户 id（非空）
     * @param ctx        当前 ChannelHandlerContext（非空）
     * @param deviceType 设备类型（若为 null 或空，则使用 "default"）
     */
    public void addChannel(String userId, ChannelHandlerContext ctx, String deviceType) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(ctx, "ctx");
        Channel ch = ctx.channel();
        if (!ch.isActive()) {
            log.warn("尝试绑定一个未激活的 channel: userId={}, channelId={}", userId, ch.id().asLongText());
            // 仍然继续绑定（视场景可改为直接返回）
        }
        final String dType = (deviceType == null || deviceType.isEmpty()) ? "default" : deviceType;

        // 设置 channel 属性（便于后续从 channel 获取 info）
        ch.attr(USER_ATTR).set(userId);
        ch.attr(DEVICE_ATTR).set(dType);

        // 将 channel 放入 userChannels 的 device map 中（并获取旧的 channel）
        ConcurrentHashMap<String, Channel> deviceMap =
                userChannels.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

        Channel previous = deviceMap.put(dType, ch);

        // 更新反向索引
        channelIndex.put(ch.id().asLongText(), new UserDevice(userId, dType));

        // 如果存在旧连接且不是同一个 channel，则关闭旧连接并移除其反向索引
        if (previous != null && previous != ch) {
            try {
                String prevId = previous.id().asLongText();
                channelIndex.remove(prevId);
                log.info("发现已存在旧连接，准备关闭旧连接: userId={}, deviceType={}, prevChannelId={}", userId, dType, prevId);
                // 优雅关闭旧 channel（异步）
                previous.close().addListener((ChannelFutureListener) future -> {
                    log.info("旧连接已关闭: userId={}, deviceType={}, prevChannelId={}, success={}", userId, dType, prevId, future.isSuccess());
                });
            } catch (Exception e) {
                log.warn("关闭旧连接时出错: userId={}, deviceType={}", userId, dType, e);
            }
        }

        // 注册 channel 关闭时的清理回调（幂等）
        ch.closeFuture().addListener(cf -> {
            try {
                // 在 close 回调中再次移除（防止竞态）
                removeByChannel(ch);
            } catch (Exception ex) {
                log.debug("channel close cleanup failed for {}: {}", ch.id().asLongText(), ex.getMessage());
            }
        });

        log.info("bind channel -> userId={}, deviceType={}, channelId={}", userId, dType, ch.id().asLongText());
    }

    /**
     * 按 userId 与 deviceType 获取 Channel（可能为 null）
     */
    public Channel getChannel(String userId, String deviceType) {
        if (userId == null || deviceType == null) return null;
        Map<String, Channel> dm = userChannels.get(userId);
        return dm == null ? null : dm.get(deviceType);
    }

    /**
     * 按 userId 获取该用户所有 device 的 Channel（只读视图）
     */
    public Collection<Channel> getChannelsByUser(String userId) {
        if (userId == null) return Collections.emptyList();
        Map<String, Channel> dm = userChannels.get(userId);
        if (dm == null) return Collections.emptyList();
        return Collections.unmodifiableCollection(dm.values());
    }

    /**
     * 获取系统中所有活跃 Channel（只读视图）
     */
    public Collection<Channel> getAllChannels() {
        // 汇总所有 device map 的 values（注意：可能存在重复但逻辑上不应）
        ConcurrentHashMap<String, Channel> snapshot = new ConcurrentHashMap<>();
        userChannels.forEach((user, map) -> {
            map.forEach((device, ch) -> snapshot.put(ch.id().asLongText(), ch));
        });
        return Collections.unmodifiableCollection(snapshot.values());
    }

    /**
     * 按 userId + deviceType 移除并可选择关闭 channel（如果存在）
     *
     * @param userId     用户 id
     * @param deviceType 设备类型
     * @param close      是否在移除后关闭 channel
     * @return 被移除的 Channel （若不存在返回 null）
     */
    public Channel removeChannel(String userId, String deviceType, boolean close) {
        if (userId == null || deviceType == null) return null;
        ConcurrentHashMap<String, Channel> dm = userChannels.get(userId);
        if (dm == null) return null;
        Channel removed = dm.remove(deviceType);
        if (removed != null) {
            channelIndex.remove(removed.id().asLongText());
            if (dm.isEmpty()) {
                userChannels.remove(userId, dm);
            }
            if (close && removed.isActive()) {
                try {
                    removed.close().addListener((ChannelFutureListener) future -> {
                        log.info("removeChannel close result: userId={}, deviceType={}, success={}", userId, deviceType, future.isSuccess());
                    });
                } catch (Exception e) {
                    log.warn("关闭移除的 channel 出错 userId={}, deviceType={}", userId, deviceType, e);
                }
            }
        }
        return removed;
    }

    /**
     * 通过 Channel 对象反向移除映射（通常由 closeFuture 的回调触发）
     *
     * @param channel 要移除的 channel
     * @return true 表示曾存在并被移除
     */
    public boolean removeByChannel(Channel channel) {
        if (channel == null) return false;
        String chId = channel.id().asLongText();
        UserDevice ud = channelIndex.remove(chId);
        if (ud == null) {
            // 可能已经被主动 remove
            return false;
        }
        ConcurrentHashMap<String, Channel> dm = userChannels.get(ud.getUserId());
        if (dm != null) {
            dm.remove(ud.getDeviceType(), channel);
            if (dm.isEmpty()) {
                userChannels.remove(ud.getUserId(), dm);
            }
        }
        log.info("channel cleanup removed mapping: userId={}, deviceType={}, channelId={}", ud.getUserId(), ud.getDeviceType(), chId);
        return true;
    }

    /**
     * 给指定用户的（指定设备或所有设备）发送消息（异步）
     *
     * @param userId     目标用户
     * @param deviceType 设备类型，若为 null 则发送给该用户所有设备
     * @param data       要发送的数据（直接传给 Channel.writeAndFlush）
     * @return 发送到的 channel 数量
     */
    public int sendToUser(String userId, String deviceType, Object data) {
        if (userId == null) return 0;
        if (deviceType == null) {
            Collection<Channel> channels = getChannelsByUser(userId);
            channels.forEach(ch -> {
                if (ch.isActive()) ch.writeAndFlush(data);
            });
            return channels.size();
        } else {
            Channel ch = getChannel(userId, deviceType);
            if (ch != null && ch.isActive()) {
                ch.writeAndFlush(data);
                return 1;
            }
            return 0;
        }
    }

    /**
     * 查询当前在线用户数（有至少一个 device 在线即计为在线）
     */
    public int getOnlineUserCount() {
        return userChannels.size();
    }

    /**
     * 查询当前总连接数（所有用户的所有设备连接之和）
     */
    public int getTotalConnectionCount() {
        int sum = 0;
        for (ConcurrentHashMap<String, Channel> dm : userChannels.values()) {
            sum += dm.size();
        }
        return sum;
    }

    /**
     * 内部用的 POJO：记录 channelId -> userId/deviceType 的映射
     */
    @Data
    private static class UserDevice {
        private final String userId;
        private final String deviceType;
    }
}
