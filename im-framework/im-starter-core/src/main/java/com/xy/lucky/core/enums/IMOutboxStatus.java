package com.xy.lucky.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 发件箱状态枚举
 * <p>
 * 用于消息发件箱的状态管理
 */
@Getter
@NoArgsConstructor
public enum IMOutboxStatus {

    PENDING(0, "待发送"),
    SUCCESS(1, "发送成功"),
    FAILED(2, "发送失败");

    private Integer code;
    private String desc;

    IMOutboxStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 状态码
     * @return 对应枚举，未找到返回 null
     */
    public static IMOutboxStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IMOutboxStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为待发送状态
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 判断是否为发送成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为发送失败状态
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}

