package com.xy.lucky.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum IMessageContentType {

    /**
     * 系统 / 提示
     */
    TIP(0, "系统提示"),

    /**
     * 文本类（1-99）
     */
    TEXT(1, "纯文本"),
    MARKDOWN(2, "Markdown 文本"),
    RICH_TEXT(3, "富文本（带样式/HTML）"),

    /**
     * 媒体（100-199）
     */
    IMAGE(100, "图片"),
    GIF(101, "动画图片(GIF/WebP)"),
    VIDEO(110, "视频"),
    AUDIO(120, "语音/音频"),
    STICKER(130, "贴纸 / 表情包"),

    /**
     * 文件 / 二进制（200-299）
     */
    FILE(200, "文件（通用）"),
    ARCHIVE(201, "压缩包"),
    DOCUMENT(202, "文档（pdf/doc/xlsx 等）"),

    /**
     * 富媒体 / 结构化内容（300-399）
     */
    LOCATION(300, "位置 / 地理位置信息"),
    CONTACT_CARD(310, "名片 / 联系人卡片"),
    URL_PREVIEW(320, "链接预览（网页摘要）"),
    POLL(330, "投票 / 问卷"),
    FORWARD(340, "转发内容（封装）"),

    /**
     * 群组（400-499）
     * 注：群组操作类型通过 GroupOperationMessageBody.operationType 细分
     */
    CREATE_GROUP(400, "创建群组"),
    INVITE_TO_GROUP(401, "群组邀请"),
    JOIN_GROUP(402, "成员加入群组"),
    LEAVE_GROUP(403, "主动退出群组"),
    KICK_FROM_GROUP(404, "移除群成员"),
    PROMOTE_TO_ADMIN(405, "设置管理员"),
    DEMOTE_FROM_ADMIN(406, "取消管理员"),
    TRANSFER_GROUP_OWNER(407, "移交群主"),
    SET_GROUP_INFO(408, "修改群信息"),
    SET_GROUP_ANNOUNCEMENT(409, "设置群公告"),
    SET_GROUP_JOIN_MODE(410, "设置群加入方式"),
    APPROVE_JOIN_REQUEST(411, "批准入群申请"),
    REJECT_JOIN_REQUEST(412, "拒绝入群申请"),
    JOIN_APPROVE_GROUP(413, "群组加入审批"),
    JOIN_APPROVE_RESULT_GROUP(414, "群组加入审批结果"),
    MUTE_MEMBER(415, "单人禁言"),
    UNMUTE_MEMBER(416, "取消禁言"),
    MUTE_ALL(417, "全员禁言"),
    UNMUTE_ALL(418, "取消全员禁言"),
    SET_MEMBER_ROLE(419, "设置群成员角色"),
    REMOVE_GROUP(420, "解散/删除群组"),
    GROUP_OPERATION(421, "群组操作（通用）"),

    /**
     * 消息操作 (450-499)
     */
    SEND_MESSAGE(450, "发送消息"),
    EDIT_MESSAGE(451, "编辑消息"),
    DELETE_MESSAGE(452, "删除消息"),
    RECALL_MESSAGE(453, "撤回消息"),
    REPLY_MESSAGE(454, "回复消息"),
    FORWARD_MESSAGE(455, "转发消息"),
    MARK_READ(456, "已读回执"),
    TYPING(457, "正在输入"),
    MESSAGE_QUOTE(458, "引用消息"),
    MERGE_MESSAGE(459, "合并消息"),


    /**
     * 其它 / 保留
     */
    COMPLEX(500, "混合消息（多类型组合）"),


    /**
     * 未知类型（保底）
     */
    UNKNOWN(999, "未知类型（保底）");

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
