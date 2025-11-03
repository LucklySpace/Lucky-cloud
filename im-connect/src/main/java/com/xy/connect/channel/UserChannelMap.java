package com.xy.connect.channel;

import com.xy.connect.config.LogConstant;
import com.xy.connect.domain.IMUserChannel;
import com.xy.connect.domain.IMUserChannel.UserChannel;
import com.xy.core.constants.IMConstant;
import com.xy.core.enums.IMDeviceType;
import com.xy.spring.annotations.core.Component;
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

        // 收集需要被踢掉的旧连接（同组互斥或相同 deviceType）
        List<UserChannel> toKick = imUserChannel.getUserChannelMap().values().stream()
                .filter(uc -> uc != null && uc.getDeviceType() != null && uc.getChannel() != null)
                .filter(uc -> !uc.getChannel().id().asLongText().equals(ch.id().asLongText()))
                .filter(uc -> uc.getDeviceType().isConflicting(dt) || uc.getDeviceType() == dt)
                .toList();

        // 逐个移除并优雅关闭旧连接
        toKick.forEach(old -> {
            try {
                String prevId = old.getChannel().id().asLongText();
                imUserChannel.getUserChannelMap().remove(old.getDeviceType().getType(), old);
                old.getChannel().close().addListener((ChannelFutureListener) future -> 
                    log.info("被替换旧连接已关闭: userId={}, deviceType={}, prevChannelId={}, success={}",
                            userId, old.getDeviceType().getType(), prevId, future.isSuccess()));
            } catch (Exception ex) {
                log.warn("关闭替换旧连接时出错 userId={}, deviceType={}", userId, old.getDeviceType(), ex);
            }
        });

        // 放入新连接并注册 closeFuture 清理
        UserChannel newUc = new UserChannel(ch.id().asLongText(), dt, ch);
        imUserChannel.getUserChannelMap().put(IMDeviceType.getByDevice(deviceKey), newUc);

        // 记录连接信息
        log.info("添加用户通道: userId={}, deviceType={}, channelId={}", userId, dt.getType(), ch.id().asLongText());

        // 注册关闭监听器，确保资源清理
        ch.closeFuture().addListener(future -> {
            try {
                removeByChannel(ch);
            } catch (Exception e) {
                log.debug("channel close cleanup failed for {}: {}", ch.id().asLongText(), e.getMessage());
            }
        });

        log.info("绑定 channel -> userId={}, deviceType={}, channelId={}", userId, deviceKey, ch.id().asLongText());
    }

    /**
     * 获取用户通道
     */
    public Channel getChannel(String userId, String deviceTypeString) {
        if (userId == null || deviceTypeString == null) return null;
        IMUserChannel im = userChannels.get(userId);
        if (im == null) return null;
        UserChannel uc = im.getUserChannelMap().get(deviceTypeString);
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

        UserChannel removed = im.getUserChannelMap().remove(deviceType);
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
    public boolean removeByChannel(Channel channel) {
        if (channel == null) return false;
        final String chId = channel.id().asLongText();

        // 尝试从 channel 属性获取信息（快速路径）
        String userId = channel.attr(USER_ATTR).get();
        String deviceType = channel.attr(DEVICE_ATTR).get();

        // 如果有用户ID和设备类型，可以直接定位
        if (userId != null && deviceType != null) {
            IMUserChannel im = userChannels.get(userId);
            if (im != null) {
                UserChannel uc = im.getUserChannelMap().get(deviceType);
                if (uc != null && chId.equals(uc.getChannelId())) {
                    im.getUserChannelMap().remove(deviceType);
                    log.info("快速移除通道映射: userId={}, deviceType={}, channelId={}",
                            userId, deviceType, chId);
                    if (im.getUserChannelMap().isEmpty()) {
                        userChannels.remove(userId, im);
                    }
                    return true;
                }
            }
        }

        // 回退到全表扫描
        for (Map.Entry<String, IMUserChannel> entry : userChannels.entrySet()) {
            userId = entry.getKey();
            IMUserChannel im = entry.getValue();
            if (im == null) continue;

            // 寻找并删除匹配的 device 条目
            Iterator<Map.Entry<IMDeviceType, UserChannel>> it = im.getUserChannelMap().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<IMDeviceType, UserChannel> e = it.next();
                UserChannel uc = e.getValue();
                if (uc != null && chId.equals(uc.getChannelId())) {
                    it.remove();
                    log.info("全表扫描移除通道映射: userId={}, deviceType={}, channelId={}",
                            userId, e.getKey(), chId);
                    // 如果该用户无设备则移除用户条目
                    if (im.getUserChannelMap().isEmpty()) {
                        userChannels.remove(userId, im);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------- 发送 ----------------

    public int sendToUser(String userId, String deviceType, Object data) {
        if (userId == null) return 0;
        if (deviceType == null) {
            Collection<Channel> channels = getChannelsByUser(userId);
            channels.stream()
                    .filter(Channel::isActive)
                    .forEach(ch -> ch.writeAndFlush(data));
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

    public int sendToUser(String userId, IMDeviceType deviceType, Object data) {
        return deviceType == null ? 0 : sendToUser(userId, deviceType.getType(), data);
    }

}