package com.xy.lucky.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum IMessageContentType {

    // 消息
    TIP(0, "系统提示"),
    TEXT(1, "文字"),
    IMAGE(2, "图片"),
    VIDEO(3, "视频"),
    AUDIO(4, "语音"),
    FILE(5, "文件"),
    EMOJI(6, "表情"),
    LOCAL(7, "位置"),
    COMPLEX(8, "混合"),

    // 音视频
    RTC_CALL(101, "呼叫"),
    RTC_ACCEPT(102, "接受"),
    RTC_REJECT(103, "拒绝"),
    RTC_CANCEL(104, "取消呼叫"),
    RTC_FAILED(105, "呼叫失败"),
    RTC_HANDUP(106, "挂断"),
    RTC_CANDIDATE(107, "同步candidate"),
    RTC_OFFLINE(108, "对方离线"),

    //群组
    GROUP_NOTICE(201, "群组通知"),
    GROUP_INVITE(202, "群组邀请"),
    GROUP_JOIN_APPROVE(203, "群组加入审批"),
    GROUP_JOIN_APPROVE_RESULT(204, "群组加入审批结果"),
    GROUP_QUIT(205, "群组退出"),
    GROUP_KICK(206, "群组踢人"),

    ;

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
