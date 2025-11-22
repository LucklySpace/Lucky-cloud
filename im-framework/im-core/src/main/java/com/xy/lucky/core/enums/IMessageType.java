package com.xy.lucky.core.enums;

public enum IMessageType {

    ERROR(-1, "信息异常"),
    LOGIN_OVER(900, "登录过期"),
    REFRESHTOKEN(999, "刷新token"),
    LOGIN(1000, "登录"),
    HEART_BEAT(1001, "心跳"),
    FORCE_LOGOUT(1002, "强制下线"),
    SINGLE_MESSAGE(1003, "私聊消息"),
    GROUP_MESSAGE(1004, "群发消息"),
    VIDEO_MESSAGE(1005, "视频通话"),
    AUDIO_MESSAGE(1006, "音频通话"),


    CREATE_GROUP(1500, "创建群聊"),
    GROUP_INVITE(1501, "群聊邀请"),


    ROBOT(2000, "机器人"),
    PUBLIC(2001, "公众号"),

    MESSAGE_ACTION(3000, "消息更新"),


    ;

    private Integer code;

    private String type;

    IMessageType(Integer code, String type) {
        this.code = code;
        this.type = type;
    }

    public static IMessageType getByCode(Integer code) {
        for (IMessageType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
