package com.xy.imcore.enums;

public enum IMessageContentType {

    TEXT(1, "文字"),
    IMAGE(2, "图片"),
    VIDEO(3, "视频"),
    AUDIO(4, "语音"),
    FILE(5, "文件"),
    LOCAL(6, "位置"),
    TIP(10, "系统提示"),

    RTC_CALL(101, "呼叫"),
    RTC_ACCEPT(102, "接受"),
    RTC_REJECT(103, "拒绝"),
    RTC_CANCEL(104, "取消呼叫"),
    RTC_FAILED(105, "呼叫失败"),
    RTC_HANDUP(106, "挂断"),
    RTC_CANDIDATE(107, "同步candidate");


    private Integer code;

    private String type;

    IMessageContentType(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static IMessageContentType getByCode(Integer code) {
        for (IMessageContentType v : values()) {
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
