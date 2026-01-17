package com.xy.lucky.connect.channel;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.connect.domain.IMUserChannel;
import com.xy.lucky.connect.domain.IMUserChannel.UserChannel;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.IMDeviceType;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户 -> 多设备 Channel 管理
 * - 同组设备互斥由 IMDeviceType.isConflicting 决定
 * - 新连接会替换冲突或相同类型的旧连接并优雅关闭旧连接
 * - Channel.closeFuture 注册幂等清理
 */
@Slf4j(topic = LogConstant.Channel)
@Component
public class UserChannelMap {

    private static final AttributeKey<String> USER_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);
    private static final AttributeKey<String> DEVICE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);

    // 用户 -> 多设备映射
    private final ConcurrentHashMap<String, IMUserChannel> userChannels = new ConcurrentHashMap<>();

    @Autowired
    private NettyProperties nettyProperties;

    /**
     * 添加并绑定用户通道
     * @param userId 用户ID
     * @param ch     Netty Channel
     * @param deviceType 设备类型
     */
    public void addChannel(String userId, Channel ch, IMDeviceType deviceType) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(ch, "channel cannot be null");

        final IMDeviceType dt = deviceType == null ? IMDeviceType.WEB : deviceType;
        final IMDeviceType.DeviceGroup group = dt.getGroup();
        final String channelId = ch.id().asLongText();

        log.debug("尝试绑定用户通道: userId={}, group={}, type={}, channelId={}", userId, group, dt.getType(), channelId);

        // 1. 在 Channel 上打标签，便于后续清理和诊断
        ch.attr(USER_ATTR).set(userId);
        ch.attr(DEVICE_ATTR).set(dt.getType());

        // 2. 获取或创建用户的通道管理对象
        IMUserChannel imUserChannel = userChannels.computeIfAbsent(userId, k -> new IMUserChannel(userId, new ConcurrentHashMap<>()));

        // 3. 处理同组互斥：如果该分组已存在其他连接，则踢出
        UserChannel existing = imUserChannel.getUserChannelMap().get(group);
        if (existing != null && !channelId.equals(existing.getChannelId())) {
            log.info("触发同组互斥踢人: userId={}, group={}, oldChannelId={}, newChannelId={}", userId, group, existing.getChannelId(), channelId);
            imUserChannel.getUserChannelMap().remove(group);
            safeKickAndClose(userId, existing, "同类型设备登录，您已被强制下线");
        }

        // 4. 如果全局配置了单设备登录 (即关闭多端支持)，则清理所有其他分组的连接
        if (Boolean.FALSE.equals(nettyProperties.getMultiDeviceEnabled())) {
            imUserChannel.getUserChannelMap().forEach((g, uc) -> {
                if (g != group) {
                    log.info("触发全局单点登录踢人: userId={}, group={}, kickedGroup={}", userId, group, g);
                    imUserChannel.getUserChannelMap().remove(g);
                    safeKickAndClose(userId, uc, "账号在其他端登录，您已被强制下线");
                }
            });
        }

        // 5. 保存新连接并注册资源回收监听
        UserChannel newUc = new UserChannel(channelId, dt, group, ch);
        imUserChannel.getUserChannelMap().put(group, newUc);

        ch.closeFuture().addListener(future -> removeByChannel(ch));

        log.info("用户通道绑定成功: userId={}, group={}, type={}", userId, group, dt.getType());
    }

    /**
     * 获取用户在特定设备分组上的 Channel
     */
    public Channel getChannel(String userId, IMDeviceType deviceType) {
        if (userId == null || deviceType == null) return null;
        IMUserChannel im = userChannels.get(userId);
        if (im == null) return null;
        UserChannel uc = im.getUserChannelMap().get(deviceType.getGroup());
        return uc != null ? uc.getChannel() : null;
    }

    /**
     * 获取用户所有在线 Channel
     */
    public Collection<Channel> getChannelsByUser(String userId) {
        if (userId == null) return Collections.emptyList();
        IMUserChannel im = userChannels.get(userId);
        if (im == null) return Collections.emptyList();

        return im.getUserChannelMap().values().stream()
                .map(UserChannel::getChannel)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 移除用户特定分组的通道
     */
    public void removeChannel(String userId, String deviceTypeStr, boolean close) {
        if (userId == null || deviceTypeStr == null) return;
        IMUserChannel im = userChannels.get(userId);
        if (im == null) return;

        IMDeviceType dt = IMDeviceType.ofOrDefault(deviceTypeStr, IMDeviceType.WEB);
        UserChannel removed = im.getUserChannelMap().remove(dt.getGroup());

        if (removed != null && close) {
            removed.getChannel().close();
        }

        if (im.getUserChannelMap().isEmpty()) {
            userChannels.remove(userId);
        }
    }

    /**
     * 根据 Channel 实例清理资源 (通常由 closeFuture 触发)
     */
    public void removeByChannel(Channel channel) {
        if (channel == null) return;
        String userId = channel.attr(USER_ATTR).get();
        String type = channel.attr(DEVICE_ATTR).get();

        if (userId != null && type != null) {
            IMUserChannel im = userChannels.get(userId);
            if (im != null) {
                IMDeviceType.DeviceGroup group = IMDeviceType.getDeviceGroupOrDefault(type, IMDeviceType.DeviceGroup.WEB);
                UserChannel uc = im.getUserChannelMap().get(group);
                // 只有当 Map 中的 Channel ID 与当前关闭的一致时才移除，防止误删新连接
                if (uc != null && channel.id().asLongText().equals(uc.getChannelId())) {
                    im.getUserChannelMap().remove(group);
                    if (im.getUserChannelMap().isEmpty()) {
                        userChannels.remove(userId);
                    }
                    log.debug("已清理离线通道: userId={}, type={}, group={}", userId, type, group);
                }
            }
        }
    }

    /**
     * 优雅地向旧连接发送踢人指令并关闭
     */
    private void safeKickAndClose(String userId, UserChannel oldUc, String reason) {
        Channel ch = oldUc.getChannel();
        if (ch == null || !ch.isActive()) return;

        try {
            IMessageWrap<Object> kickMsg = new IMessageWrap<>()
                    .setCode(IMessageType.FORCE_LOGOUT.getCode())
                    .setMessage(reason);

            // 发送消息并添加监听器，消息发送完成后关闭连接
            ch.writeAndFlush(kickMsg).addListener(future -> {
                log.info("踢人指令已送达，关闭旧连接: userId={}, group={}", userId, oldUc.getGroup());
                ch.close();
            });
        } catch (Exception e) {
            log.error("发送踢人消息异常: userId={}, group={}", userId, oldUc.getGroup(), e);
            ch.close();
        }
    }

    // --- 统计相关的简易方法 ---

    public int getOnlineUserCount() {
        return userChannels.size();
    }

    public int getTotalConnectionCount() {
        return userChannels.values().stream().mapToInt(im -> im.getUserChannelMap().size()).sum();
    }
}
