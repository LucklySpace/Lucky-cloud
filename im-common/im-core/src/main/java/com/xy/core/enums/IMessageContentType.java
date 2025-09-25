package com.xy.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum IMessageContentType {

    TEXT(1, "文字"),
    IMAGE(2, "图片"),
    VIDEO(3, "视频"),
    AUDIO(4, "语音"),
    FILE(5, "文件"),
    LOCAL(6, "位置"),
    COMPLEX(7, "混合"),
    GROUP_INVITE(8, "群组邀请"),
    GROUP_JOIN_APPROVE(9, "群组加入审批"),


    TIP(10, "系统提示"),


    RTC_CALL(101, "呼叫"),
    RTC_ACCEPT(102, "接受"),
    RTC_REJECT(103, "拒绝"),
    RTC_CANCEL(104, "取消呼叫"),
    RTC_FAILED(105, "呼叫失败"),
    RTC_HANDUP(106, "挂断"),
    RTC_CANDIDATE(107, "同步candidate");

    private Integer code;

    private String desc;

    public static IMessageContentType getByCode(Integer code) {
        for (IMessageContentType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

}
