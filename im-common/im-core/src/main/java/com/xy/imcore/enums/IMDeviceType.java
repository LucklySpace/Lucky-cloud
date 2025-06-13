package com.xy.imcore.enums;

public enum IMDeviceType {

    ANDROID("android"),
    IOS("ios"),
    WEB("web"),
    MAC("mac"),
    WIN("win"),
    LINUX("linux");

    private final String device;

    IMDeviceType(String device) {
        this.device = device;
    }
}
