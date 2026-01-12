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
     */
    GROUP_INVITE(400, "群组邀请"),


    /**
     * 其它 / 保留
     */
    COMPLEX(500, "混合消息（多类型组合）"),
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
