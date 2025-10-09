package com.xy.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public enum IMDeviceType {

    ANDROID("android"),
    IOS("ios"),
    WEB("web"),
    MAC("mac"),
    WIN("win"),
    LINUX("linux");

    private String device;

    IMDeviceType(String device) {
        this.device = device;
    }

    public static IMDeviceType getByDevice(String device) {
        for (IMDeviceType type : IMDeviceType.values()) {
            if (type.device.equals(device)) {
                return type;
            }
        }
        return null;
    }
}
