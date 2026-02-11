package com.xy.lucky.connect.domain;

import com.xy.lucky.core.enums.IMDeviceType;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * IM 用户通道管理类，包含用户 ID 和其在不同设备上的 Channel 映射
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IMUserChannel {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户设备通道映射：设备分组 -> 用户通道详情
     * - 设备分组（MOBILE/DESKTOP/WEB）作为 Key 确保同组互斥
     */
    private Map<IMDeviceType.DeviceGroup, UserChannel> userChannelMap;

    /**
     * 获取用户在特定分组上的在线通道
     */
    public UserChannel getChannelByGroup(IMDeviceType.DeviceGroup group) {
        return userChannelMap != null ? userChannelMap.get(group) : null;
    }

    /**
     * 设备通道详情
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserChannel {
        /**
         * Netty Channel ID (LongText)
         */
        private String channelId;

        /**
         * 具体设备类型（如 ANDROID, IOS, WIN）
         */
        private IMDeviceType deviceType;

        /**
         * 所属设备分组（如 MOBILE, DESKTOP, WEB）
         */
        private IMDeviceType.DeviceGroup group;

        /**
         * Netty Channel 实例
         */
        private Channel channel;
    }
}
