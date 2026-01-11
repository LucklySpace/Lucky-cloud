package com.xy.lucky.core.enums;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IM 操作类型枚举（重构版）
 * <p>
 * 设计原则：
 * - 使用固定编号区间便于分组（例如 1-99 为消息相关，200-299 为群组成员操作，500-599 为通话相关等）
 * - 保证 code 唯一
 * - 提供从 code 查 enum 的高效方法与一些常用分类判断方法
 */
@Getter
public enum IMActionType {

    /**
     * 消息相关（1 - 99）
     */
    SEND_MESSAGE(1, "发送消息"),
    EDIT_MESSAGE(2, "编辑消息"),
    DELETE_MESSAGE(3, "删除消息"),
    RECALL_MESSAGE(4, "撤回消息"),
    REPLY_MESSAGE(5, "回复消息"),
    FORWARD_MESSAGE(6, "转发消息"),
    MARK_READ(7, "已读回执"),
    TYPING(8, "正在输入"),
    MESSAGE_QUOTE(9, "引用消息"),

    /**
     * 表情 /反应（70+）
     */
    REACTION_ADD(70, "添加表情反应"),
    REACTION_REMOVE(71, "移除表情反应"),

    /**
     * 群组成员 / 权限操作（200 - 299）
     */
    CREATE_GROUP(200, "创建群组"),
    INVITE_TO_GROUP(201, "群组邀请"),
    JOIN_GROUP(202, "成员加入群组"),
    LEAVE_GROUP(203, "主动退出群组"),
    KICK_FROM_GROUP(204, "移除群成员"),
    PROMOTE_TO_ADMIN(205, "设置管理员"),
    DEMOTE_FROM_ADMIN(206, "取消管理员"),
    TRANSFER_GROUP_OWNER(207, "移交群主"),
    SET_GROUP_INFO(208, "修改群信息"),
    SET_GROUP_ANNOUNCEMENT(209, "设置群公告"),
    SET_GROUP_JOIN_MODE(210, "设置群加入方式"),
    APPROVE_JOIN_REQUEST(211, "批准入群申请"),
    REJECT_JOIN_REQUEST(212, "拒绝入群申请"),
    JOIN_APPROVE_GROUP(213, "群组加入审批"),
    JOIN_APPROVE_RESULT_GROUP(214, "群组加入审批结果"),
    MUTE_MEMBER(215, "单人禁言"),
    UNMUTE_MEMBER(216, "取消禁言"),
    MUTE_ALL(217, "全员禁言"),
    UNMUTE_ALL(218, "取消全员禁言"),
    SET_MEMBER_ROLE(219, "设置群成员角色"),
    REMOVE_GROUP(220, "解散/删除群组"),

    /**
     * 文件/传输（600 - 699）
     */
    UPLOAD_FILE(600, "文件上传"),
    DOWNLOAD_FILE(601, "文件下载"),
    SHARE_FILE(602, "文件分享"),
    CHUNK_UPLOAD(603, "分片上传"),
    CHUNK_COMPLETE(604, "分片合并完成"),

    /**
     * 好友 / 联系人（300 - 399）
     */
    ADD_FRIEND(300, "添加好友"),
    REMOVE_FRIEND(301, "删除好友"),
    BLOCK_USER(302, "拉黑用户"),
    UNBLOCK_USER(303, "解除拉黑"),
    FRIEND_REQUEST(304, "好友请求"),

    /**
     * 系统 / 管理（900 - 999）
     */
    SYSTEM_NOTIFICATION(900, "系统通知"),
    MODERATION_ACTION(901, "平台管理操作"),
    AUDIT_LOG(902, "审计日志记录");

    // 高效查询
    private static final Map<Integer, IMActionType> BY_CODE = new ConcurrentHashMap<>();

    static {
        for (IMActionType t : values()) {
            // 防护：防止重复 code 把后者覆盖，抛错提醒更直观（启动时即可发现）
            IMActionType prev = BY_CODE.putIfAbsent(t.code, t);
            if (prev != null) {
                throw new IllegalStateException("Duplicate IMActionType code: " + t.code + " for " + t + " and " + prev);
            }
        }
    }

    private final Integer code;
    private final String desc;

    IMActionType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举（找不到返回 null）
     */
    public static IMActionType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return BY_CODE.get(code);
    }

    /**
     * 根据 code 获取枚举，找不到则抛出异常
     */
    public static IMActionType fromCodeOrThrow(Integer code) {
        IMActionType t = fromCode(code);
        if (t == null) {
            throw new IllegalArgumentException("Unknown IMActionType code: " + code);
        }
        return t;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return code + ":" + name() + "(" + desc + ")";
    }
}
