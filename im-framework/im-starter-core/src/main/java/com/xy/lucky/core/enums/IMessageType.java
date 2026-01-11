package com.xy.lucky.core.enums;

public enum IMessageType {

    /*
     * 协议 / 错误
     */
    ERROR(-1, "协议错误/非法数据包"),
    SUCCESS(0, "成功响应"),

    /*
     * 鉴权 / 会话（0 - 99）
     */
    LOGIN(1, "登录"),
    LOGOUT(2, "退出登录"),
    LOGIN_EXPIRED(3, "登录过期"),
    REFRESH_TOKEN(4, "刷新 Token"),
    FORCE_LOGOUT(5, "强制下线"),
    TOKEN_ERROR(6, "Token 错误"),
    NOT_LOGIN(7, "未登录"),

    /*
     * 连接控制（100 - 199）
     */
    REGISTER(100, "用户注册"),
    HEART_BEAT(101, "心跳"),
    CONNECT(102, "建立连接"),
    DISCONNECT(103, "断开连接"),
    DUPLICATE_LOGIN(104, "异地登录"),
    PRESENCE_UPDATE(105, "在线状态更新"),
    LAST_SEEN_UPDATE(106, "最后在线时间更新"),
    LOGIN_FAILED_TOO_MANY_TIMES(107, "登录失败次数过多"),
    REGISTER_SUCCESS(120, "注册成功"),
    REGISTER_FAILED(121, "注册失败"),
    HEART_BEAT_SUCCESS(130, "心跳成功"),
    HEART_BEAT_FAILED(131, "心跳失败"),


    /*
     * 音视频（500 - 599）
     */
    RTC_START_AUDIO_CALL(500, "发起语音通话"),
    RTC_START_VIDEO_CALL(501, "发起视频通话"),
    RTC_ACCEPT(502, "接受通话"),
    RTC_REJECT(503, "拒绝通话"),
    RTC_CANCEL(504, "取消通话"),
    RTC_FAILED(505, "通话失败"),
    RTC_HANDUP(506, "挂断通话"),
    RTC_CANDIDATE(507, "同步candidate"),
    RTC_OFFLINE(508, "对方离线"),

    /*
     * 消息投递（1000 - 1999）
     */
    SINGLE_MESSAGE(1000, "单聊消息"),
    GROUP_MESSAGE(1001, "群聊消息"),
    VIDEO_MESSAGE(1002, "视频消息"),
    SYSTEM_MESSAGE(1003, "系统消息"),
    BROADCAST_MESSAGE(1004, "广播消息"),

    /*
     * 会话对象 / 虚拟主体（2000 - 2999）
     */
    USER(2000, "普通用户"),
    ROBOT(2001, "机器人"),
    PUBLIC_ACCOUNT(2002, "公众号"),
    CUSTOMER_SERVICE(2003, "客服"),

    /*
     * 系统保留（9000+）
     */
    UNKNOWN(9999, "未知指令");
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
