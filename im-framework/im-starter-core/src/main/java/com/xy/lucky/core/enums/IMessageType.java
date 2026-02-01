package com.xy.lucky.core.enums;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * IMessageType - IM 协议/事件类型枚举
 */
public enum IMessageType {

    /* System / Protocol */
    ERROR(-1, "协议错误/非法数据包"),
    SUCCESS(0, "成功响应"),

    /* Authentication (100 - 199) */
    LOGIN(100, "登录"),
    LOGOUT(101, "退出登录"),
    LOGIN_EXPIRED(102, "登录过期"),
    REFRESH_TOKEN(103, "刷新 Token"),
    FORCE_LOGOUT(104, "强制下线"),
    TOKEN_ERROR(105, "Token 错误"),
    NOT_LOGIN(106, "未登录"),
    LOGIN_FAILED_TOO_MANY_TIMES(107, "登录失败次数过多"),

    /* Connection / Session (200 - 299) */
    REGISTER(200, "用户注册"),
    CONNECT(201, "建立连接"),
    DISCONNECT(202, "断开连接"),
    DUPLICATE_LOGIN(203, "异地登录"),
    PRESENCE_UPDATE(204, "在线状态更新"),
    LAST_SEEN_UPDATE(205, "最后在线时间更新"),
    HEART_BEAT(206, "心跳"),
    HEART_BEAT_SUCCESS(207, "心跳成功"),
    HEART_BEAT_FAILED(208, "心跳失败"),
    REGISTER_SUCCESS(209, "注册成功"),
    REGISTER_FAILED(210, "注册失败"),

    /* Reactions (470 - 479) */
    REACTION_ADD(400, "添加表情反应"),
    REACTION_REMOVE(401, "移除表情反应"),

    /* Entity types (500 - 599) */
    USER(500, "普通用户"),
    ROBOT(501, "机器人"),
    PUBLIC_ACCOUNT(502, "公众号"),
    CUSTOMER_SERVICE(503, "客服"),

    /* Friend / Social (550 - 599) */
    ADD_FRIEND(550, "添加好友"),
    REMOVE_FRIEND(551, "删除好友"),
    BLOCK_USER(552, "拉黑用户"),
    UNBLOCK_USER(553, "解除拉黑"),
    FRIEND_REQUEST(554, "好友请求"),

    /* File operations (600 - 699) */
    UPLOAD_FILE(600, "文件上传"),
    DOWNLOAD_FILE(601, "文件下载"),
    SHARE_FILE(602, "文件分享"),
    CHUNK_UPLOAD(603, "分片上传"),
    CHUNK_COMPLETE(604, "分片合并完成"),

    /* RTC / Audio-Video (700 - 799) */
    RTC_START_AUDIO_CALL(700, "发起语音通话"),
    RTC_START_VIDEO_CALL(701, "发起视频通话"),
    RTC_ACCEPT(702, "接受通话"),
    RTC_REJECT(703, "拒绝通话"),
    RTC_CANCEL(704, "取消通话"),
    RTC_FAILED(705, "通话失败"),
    RTC_HANGUP(706, "挂断通话"),
    RTC_CANDIDATE(707, "同步 candidate"),
    RTC_OFFLINE(708, "对方离线"),

    /* Notification / Audit (900 - 999) */
    SYSTEM_NOTIFICATION(900, "系统通知"),
    MODERATION_ACTION(901, "平台管理操作"),
    AUDIT_LOG(902, "审计日志记录"),


    /* Message types (1000 - ?) */
    SINGLE_MESSAGE(1000, "单聊消息"),
    GROUP_MESSAGE(1001, "群聊消息"),
    VIDEO_MESSAGE(1002, "视频消息"),
    SYSTEM_MESSAGE(1003, "系统消息"),
    BROADCAST_MESSAGE(1004, "广播消息"),
    GROUP_OPERATION(1005, "群组操作（通用）"),
    MESSAGE_OPERATION(1006, "消息操作 (通用)"),

    /* Reserved / Unknown */
    UNKNOWN(9999, "未知指令");

    /**
     * 快速查找 map（初始化一次）
     */
    private static final Map<Integer, IMessageType> CODE_MAP =
            Stream.of(IMessageType.values()).collect(Collectors.toMap(IMessageType::getCode, e -> e));
    private final Integer code;
    private final String description;

    IMessageType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 获取枚举（Optional 风格）
     */
    public static Optional<IMessageType> fromCode(Integer code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(CODE_MAP.get(code));
    }

    public static IMessageType getByCode(Integer code) {
        for (IMessageType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /**
     * 如果你习惯直接拿到枚举或 UNKNOWN（非空）
     */
    public static IMessageType fromCodeOrUnknown(Integer code) {
        return fromCode(code).orElse(UNKNOWN);
    }

    public Integer getCode() {
        return code;
    }

    private static boolean inRange(int v, int from, int to) {
        return v >= from && v <= to;
    }

    public String getDescription() {
        return description;
    }

    /* ---------- 便捷判断方法（按范围判定） ---------- */

    @Override
    public String toString() {
        return code + ":" + name() + "(" + description + ")";
    }

    public Category getCategory() {
        int c = this.code;
        if (c <= 0) return Category.SYSTEM;
        if (inRange(c, 100, 199)) return Category.AUTH;
        if (inRange(c, 200, 299)) return Category.CONNECTION;
        if (inRange(c, 300, 399)) return Category.GROUP;
        if (inRange(c, 400, 499)) return Category.MESSAGE;
        if (inRange(c, 500, 599)) return Category.ENTITY;
        if (inRange(c, 600, 699)) return Category.FILE;
        if (inRange(c, 700, 799)) return Category.RTC;
        if (inRange(c, 900, 999)) return Category.NOTIFICATION;
        return Category.RESERVED;
    }

    public boolean isMessage() {
        return getCategory() == Category.MESSAGE;
    }

    public boolean isGroup() {
        return getCategory() == Category.GROUP;
    }

    public boolean isAuth() {
        return getCategory() == Category.AUTH;
    }

    public boolean isRtc() {
        return getCategory() == Category.RTC;
    }

    public enum Category {
        SYSTEM,
        AUTH,
        CONNECTION,
        GROUP,
        MESSAGE,
        ENTITY,
        FILE,
        RTC,
        NOTIFICATION,
        RESERVED
    }
}
