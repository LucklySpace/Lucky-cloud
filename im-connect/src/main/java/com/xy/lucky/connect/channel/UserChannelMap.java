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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
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
     * 添加用户通道
     * - 默认到 WEB
     * - 新连接会替换冲突或相同类型的旧连接并优雅关闭旧连接
     * - Channel.closeFuture 注册幂等清理
     */
    public void addChannel(String userId, ChannelHandlerContext ctx, String deviceTypeString) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(ctx, "ctx");
        addChannel(userId, ctx.channel(), IMDeviceType.getByDevice(deviceTypeString));
    }

    /**
     * 添加用户通道
     * - 新连接会替换冲突或相同类型的旧连接并优雅关闭旧连接
     * - Channel.closeFuture 注册幂等清理
     */
    public void addChannel(String userId, Channel ch, IMDeviceType deviceType) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(ch, "channel");

        // 默认到 WEB
        final IMDeviceType dt = deviceType == null ? IMDeviceType.WEB : deviceType;
        final String deviceKey = dt.getType();
        final IMDeviceType.DeviceGroup group = dt.getGroup();

        if (!ch.isActive()) {
            log.warn("绑定未激活 channel: userId={}, channelId={}", userId, ch.id().asLongText());
        }

        // 在 channel 上标记信息，方便调试/诊断
        ch.attr(USER_ATTR).set(userId);
        ch.attr(DEVICE_ATTR).set(deviceKey);

        // 获取或创建用户映射
        IMUserChannel imUserChannel = userChannels.computeIfAbsent(userId, k -> {
            IMUserChannel u = new IMUserChannel();
            u.setUserId(userId);
            u.setUserChannelMap(new ConcurrentHashMap<>());
            return u;
        });

        if (!Boolean.TRUE.equals(nettyProperties.getMultiDeviceEnabled())) {
            if (!imUserChannel.getUserChannelMap().isEmpty()) {
                for (UserChannel old : imUserChannel.getUserChannelMap().values()) {
                    if (old == null || old.getChannel() == null) {
                        continue;
                    }
                    if (old.getChannel().id().asLongText().equals(ch.id().asLongText())) {
                        continue;
                    }
                    safeKickAndClose(userId, old);
                }
                imUserChannel.getUserChannelMap().clear();
            }
        } else {
            UserChannel existing = imUserChannel.getUserChannelMap().get(group);
            if (existing != null && existing.getChannel() != null
                    && !existing.getChannel().id().asLongText().equals(ch.id().asLongText())) {
                imUserChannel.getUserChannelMap().remove(group, existing);
                safeKickAndClose(userId, existing);
            }
        }

        UserChannel newUc = new UserChannel(ch.id().asLongText(), dt, group, ch);
        imUserChannel.getUserChannelMap().put(group, newUc);

        // 注册关闭监听器，确保资源清理
        ch.closeFuture().addListener(future -> {
            try {
                removeByChannel(ch);
            } catch (Exception e) {
                log.debug("channel close cleanup failed for {}: {}", ch.id().asLongText(), e.getMessage());
            }
        });

        log.info("绑定 channel -> userId={}, group={}, deviceType={}, channelId={}", userId, group.name(), deviceKey, ch.id().asLongText());
    }

    /**
     * 获取用户通道
     */
    public Channel getChannel(String userId, String deviceTypeString) {
        if (userId == null || deviceTypeString == null) return null;
        IMUserChannel im = userChannels.get(userId);
        if (im == null) return null;
        IMDeviceType dt = IMDeviceType.ofOrDefault(deviceTypeString, IMDeviceType.WEB);
        final IMDeviceType.DeviceGroup group = dt.getGroup();
        UserChannel uc = im.getUserChannelMap().get(group);
        return uc == null ? null : uc.getChannel();
    }

    /**
     * 获取用户通道
     */
    public Channel getChannel(String userId, IMDeviceType deviceType) {
        return deviceType == null ? null : getChannel(userId, deviceType.getType());
    }

    /**
     * 获取用户所有通道
     */
    public Collection<Channel> getChannelsByUser(String userId) {
        if (userId == null) return Collections.emptyList();
        IMUserChannel im = userChannels.get(userId);
        if (im == null) return Collections.emptyList();
        
        List<Channel> channels = im.getUserChannelMap().values().stream()
                .filter(uc -> uc != null && uc.getChannel() != null)
                .map(UserChannel::getChannel)
                .toList();
                
        return Collections.unmodifiableCollection(channels);
    }

    /**
     * 获取所有通道
     */
    public Collection<Channel> getAllChannels() {
        Map<String, Channel> snapshot = new HashMap<>();
        userChannels.forEach((uid, im) -> 
            im.getUserChannelMap().forEach((k, uc) -> {
                if (uc != null && uc.getChannel() != null) {
                    snapshot.put(uc.getChannel().id().asLongText(), uc.getChannel());
                }
            })
        );
        return Collections.unmodifiableCollection(snapshot.values());
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return userChannels.size();
    }

    public ConcurrentHashMap<String, IMUserChannel> getUserChannels() {
        return userChannels;
    }

    /**
     * 获取总连接数
     */
    public int getTotalConnectionCount() {
        return userChannels.values().stream()
                .mapToInt(im -> im.getUserChannelMap().size())
                .sum();
    }

    // ---------------- 移除 ----------------

    /**
     * 按 userId + deviceTypeString 移除（可选择关闭）
     */
    public Channel removeChannel(String userId, String deviceType, boolean close) {
        if (userId == null || deviceType == null) return null;
        IMUserChannel im = userChannels.get(userId);
        if (im == null) return null;

        IMDeviceType dt = IMDeviceType.ofOrDefault(deviceType, IMDeviceType.WEB);
        IMDeviceType.DeviceGroup group = dt.getGroup();

        UserChannel removed = im.getUserChannelMap().remove(group);
        if (removed == null) return null;

        if (im.getUserChannelMap().isEmpty()) {
            userChannels.remove(userId, im);
        }

        Channel ch = removed.getChannel();
        if (close && ch != null && ch.isActive()) {
            try {
                ch.close().addListener((ChannelFutureListener) future -> 
                    log.info("removeChannel close result: userId={}, deviceType={}, success={}", 
                            userId, deviceType, future.isSuccess()));
            } catch (Exception e) {
                log.warn("关闭移除的 channel 出错 userId={}, deviceType={}", userId, deviceType, e);
            }
        }
        return ch;
    }

    /**
     * 通过 Channel 对象扫描并移除对应映射（无索引版本）
     * - 增强版本，确保资源完全清理
     */
    public void removeByChannel(Channel channel) {
        if (channel == null) return;
        final String chId = channel.id().asLongText();

        // 尝试从 channel 属性获取信息（快速路径）
        String userId = channel.attr(USER_ATTR).get();
        String deviceType = channel.attr(DEVICE_ATTR).get();

        // 如果有用户ID和设备类型，可以直接定位
        if (userId != null && deviceType != null) {
            IMUserChannel im = userChannels.get(userId);
            if (im != null) {
                IMDeviceType.DeviceGroup group = IMDeviceType.getDeviceGroupOrDefault(deviceType, IMDeviceType.DeviceGroup.DESKTOP);
                UserChannel uc = im.getUserChannelMap().get(group);
                if (uc != null && chId.equals(uc.getChannelId())) {
                    im.getUserChannelMap().remove(group);
                    log.info("快速移除通道映射: userId={}, deviceType={}, group={}, channelId={}",
                            userId, deviceType, group, chId);
                    if (im.getUserChannelMap().isEmpty()) {
                        userChannels.remove(userId, im);
                    }
                }
            }
        }
    }

    private void safeKickAndClose(String userId, UserChannel old) {
        Channel oldCh = old.getChannel();
        if (oldCh == null) return;
        try {
            oldCh.writeAndFlush(new IMessageWrap<>()
                    .setCode(IMessageType.FORCE_LOGOUT.getCode())
                    .setMessage("同端登录，已被强制下线"));
        } catch (Exception ignored) {
        }
        try {
            String prevId = oldCh.id().asLongText();
            oldCh.close().addListener((ChannelFutureListener) future ->
                    log.info("被替换旧连接已关闭: userId={}, deviceType={}, group={}, prevChannelId={}, success={}",
                            userId,
                            old.getDeviceType() != null ? old.getDeviceType().getType() : null,
                            old.getGroup(),
                            prevId,
                            future.isSuccess()));
        } catch (Exception ex) {
            log.warn("关闭替换旧连接时出错 userId={}, deviceType={}, group={}",
                    userId,
                    old.getDeviceType() != null ? old.getDeviceType().getType() : null,
                    old.getGroup(),
                    ex);
        }
    }

}
