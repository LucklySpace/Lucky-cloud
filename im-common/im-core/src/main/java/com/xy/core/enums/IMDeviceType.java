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
}
