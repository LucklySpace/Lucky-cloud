package com.xy.lucky.platform.domain.enums;

public enum NotifyStatus {
    FAILED(0),
    SUCCESS(1);

    private final int code;

    NotifyStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
