package com.xy.lucky.rpc.api.platform.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * 通知类型枚举
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
public enum NotifyType implements Serializable {

    /**
     * 邮件
     */
    EMAIL,

    /**
     * 短信
     */
    SMS;

    public static NotifyType fromValue(String value) {
        for (NotifyType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NotifyType: " + value);
    }

    @JsonValue
    @Override
    public String toString() {
        return this.name();
    }
}
