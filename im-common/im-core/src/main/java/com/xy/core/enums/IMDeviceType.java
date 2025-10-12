package com.xy.core.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 设备类型枚举，包含设备分组信息以及常用工具方法
 * <p>设备分组说明：
 * <ul>
 *   <li>MOBILE：移动端（ANDROID, IOS） — 同组互斥</li>
 *   <li>DESKTOP：桌面端（MAC, WIN, LINUX） — 同组互斥</li>
 *   <li>WEB：Web 端（WEB） — 单独一组</li>
 * </ul>
 */
public enum IMDeviceType {

    ANDROID("android", DeviceGroup.MOBILE),
    IOS("ios", DeviceGroup.MOBILE),
    WEB("web", DeviceGroup.WEB),
    MAC("mac", DeviceGroup.DESKTOP),
    WIN("win", DeviceGroup.DESKTOP),
    LINUX("linux", DeviceGroup.DESKTOP);

    private final String type;
    private final DeviceGroup group;

    IMDeviceType(String type, DeviceGroup group) {
        this.type = type;
        this.group = group;
    }

    /**
     * 返回设备类型的标准字符串
     */
    @Override
    public String toString() {
        return type;
    }

    public String getType() {
        return type;
    }

    public DeviceGroup getGroup() {
        return group;
    }

    /**
     * 判断当前设备类型是否与另一个设备类型属于同一组（同组视为互斥）
     *
     * @param other 另一个设备类型
     * @return true 表示两者属于同一组（互斥），false 表示不互斥或 other 为 null
     */
    public boolean isConflicting(IMDeviceType other) {
        return other != null && this.group == other.group;
    }

    /**
     * 是否属于移动设备组（ANDROID / IOS）
     */
    public boolean isMobile() {
        return this.group == DeviceGroup.MOBILE;
    }

    /**
     * 是否属于桌面设备组（MAC / WIN / LINUX）
     */
    public boolean isDesktop() {
        return this.group == DeviceGroup.DESKTOP;
    }

    /**
     * 是否属于 web 端（WEB）
     */
    public boolean isWeb() {
        return this.group == DeviceGroup.WEB;
    }

    // 以小写 type 为 key 的查找表（不可变、线程安全）
    private static final Map<String, IMDeviceType> BY_TYPE;

    static {
        Map<String, IMDeviceType> map = new HashMap<>(IMDeviceType.values().length * 2);
        for (IMDeviceType t : IMDeviceType.values()) {
            map.put(t.type, t);
        }
        BY_TYPE = Collections.unmodifiableMap(map);
    }

    /**
     * 按设备标识解析枚举（大小写 & 前后空白容错）
     *
     * @param device 原始设备标识
     * @return IMDeviceType 或 null
     */
    public static IMDeviceType getByDevice(String device) {
        if (device == null) {
            return null;
        }
        String key = device.trim().toLowerCase();
        return key.isEmpty() ? null : BY_TYPE.get(key);
    }

    public static IMDeviceType of(String device) {
        return getByDevice(device);
    }

    public static IMDeviceType ofOrDefault(String device, IMDeviceType defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        IMDeviceType result = getByDevice(device);
        return result != null ? result : defaultValue;
    }

    /**
     * 设备分组，用于判定同组互斥关系
     */
    public enum DeviceGroup {
        MOBILE,  // 移动端设备：ANDROID, IOS
        DESKTOP, // 桌面端设备：MAC, WIN, LINUX
        WEB      // Web 端：WEB
    }
}