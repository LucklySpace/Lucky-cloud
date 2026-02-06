package com.xy.lucky.rpc.api.platform.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * 通知状态枚举
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
public enum NotifyStatus implements Serializable {

    /**
     * 失败
     */
    FAILED(0),

    /**
     * 成功
     */
    SUCCESS(1);

    private final int code;

    NotifyStatus(int code) {
        this.code = code;
    }

    public static NotifyStatus fromCode(int code) {
        for (NotifyStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown NotifyStatus code: " + code);
    }

    @JsonValue
    public int code() {
        return code;
    }
}
