package com.xy.connect.domain;

import com.xy.core.enums.IMDeviceType;
import io.netty.channel.Channel;
import lombok.*;

import java.util.Map;

/**
 * IM 用户通道管理类，包含用户 ID 和其在不同设备上的 Channel 映射
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IMUserChannel {

    // 用户id
    private String userId;

    // 用户设备通道映射：设备类型 -> UserChannel
    private Map<IMDeviceType, UserChannel> userChannelMap;

    /**
     * 判断当前设备类型是否与已有设备冲突，确保同类设备只允许一个连接
     * 
     * @param deviceType 当前设备类型
     * @return true 如果冲突，false 如果没有冲突
     */
    public boolean isDeviceConflict(IMDeviceType deviceType) {
        return userChannelMap.values().stream()
                .map(UserChannel::getDeviceType)
                .anyMatch(existing -> existing.isConflicting(deviceType));
    }

    /**
     * 设备通道类
     */
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserChannel {

        // 通道id
        private String channelId;

        // 设备类型
        private IMDeviceType deviceType;

        // 用户通道
        private Channel channel;
    }
}