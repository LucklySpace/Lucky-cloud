package com.xy.lucky.core.enums;


import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * WebRTC 通话类型枚举
 */
@Getter
@NoArgsConstructor
public enum IMWebRTCType {

    RTC_CALL(101, "呼叫"),
    RTC_ACCEPT(102, "接受"),
    RTC_REJECT(103, "拒绝"),
    RTC_CANCEL(104, "取消呼叫"),
    RTC_FAILED(105, "呼叫失败"),
    RTC_HANDUP(106, "挂断"),
    RTC_CANDIDATE(107, "同步candidate");

    private Integer code;
    private String desc;

    IMWebRTCType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static IMWebRTCType getByCode(Integer code) {
        for (IMWebRTCType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

}
