package com.xy.lucky.core.model;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.xy.lucky.core.enums.IMessageReadStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 抽象消息 DTO，定义通用属性
 *
 * @author dense
 */
@Data
@SuperBuilder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class IMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 发送者 ID
     */
    @NotBlank(message = "发送人 id 不能为空")
    private String fromId;

    /**
     * 消息临时 ID（客户端生成）
     */
    @NotBlank(message = "消息临时 id 不能为空")
    private String messageTempId;

    /**
     * 消息唯一 ID（服务端生成，可为空）
     */
    private String messageId;

    /**
     * 消息内容类型
     */
    @NotNull(message = "消息内容类型不能为空")
    private Integer messageContentType;

    /**
     * 消息发生时间戳，毫秒
     */
    @NotNull(message = "消息时间戳不能为空")
    //@MessageTimeValid
    private Long messageTime;

    /**
     * 消息阅读状态
     */
    private Integer readStatus = IMessageReadStatus.UNREAD.getCode();

    /**
     * 顺序号，用于排序
     */
    private Long sequence;

    /**
     * 额外信息（扩展字段，JSON 格式存储）
     */
    /**
     * 可扩展额外字段
     */
    private Map<String, Object> extra;

    /* ------------------- 引用消息 & @人 扩展 ------------------- */

    /**
     * 引用的消息（被回复的消息）
     */
    private ReplyMessageInfo replyMessage;

    /**
     * 被 @ 的用户 ID 列表
     */
    private List<String> mentionedUserIds = Collections.emptyList();

    /**
     * 是否 @ 所有人
     */
    private Boolean mentionAll = false;

    /* ------------------- 消息体 ------------------- */

    /**
     * 消息实体
     */
    @NotNull(message = "消息体不能为空")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageContentType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextMessageBody.class, name = "1"), // 文本消息
            @JsonSubTypes.Type(value = ImageMessageBody.class, name = "2"),// 图片消息
            @JsonSubTypes.Type(value = VideoMessageBody.class, name = "3"),// 视频消息
            @JsonSubTypes.Type(value = AudioMessageBody.class, name = "4"),// 语音消息
            @JsonSubTypes.Type(value = FileMessageBody.class, name = "5"),// 文件消息
            @JsonSubTypes.Type(value = LocationMessageBody.class, name = "6"),// 位置消息
            @JsonSubTypes.Type(value = ComplexMessageBody.class, name = "7"),// 混合消息
            @JsonSubTypes.Type(value = GroupInviteMessageBody.class, name = "8"),// 群组邀请
            @JsonSubTypes.Type(value = SystemMessageBody.class, name = "10") // 系统消息
    })
    private MessageBody messageBody;

    /* ------------------- 嵌套类 ------------------- */

    // 引用消息信息
    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyMessageInfo implements Serializable {
        private String messageId;   // 被引用消息ID
        private String fromId;      // 被引用消息的发送者
        private String previewText; // 摘要（如文本前30字、文件名、[图片]等）
        private Integer messageContentType;
    }

    // 基类
    public static abstract class MessageBody {
        // 公共字段
    }

    /**
     * 文本消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextMessageBody extends MessageBody implements Serializable {
        @NotBlank(message = "消息内容不能为空")
        private String text;
    }

    /**
     * 图片消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageMessageBody extends MessageBody implements Serializable {
        @NotBlank(message = "图片 path 不能为空")
        private String path;
        private String name;
        private Integer size;
    }

    /**
     * 视频消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoMessageBody extends MessageBody implements Serializable {

        @NotBlank(message = "视频 URL 不能为空")
        private String path;
        private String name;
        // 单位：秒
        private Integer duration;

        private Integer size;
    }

    /**
     * 语音消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioMessageBody extends MessageBody implements Serializable {

        @NotBlank(message = "视频 URL 不能为空")
        private String path;

        // 单位：秒
        private Integer duration;

        private Integer size;
    }

    /**
     * 文件消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileMessageBody extends MessageBody implements Serializable {

        @NotBlank(message = "视频 URL 不能为空")
        private String path;
        private String name;
        private String suffix;
        private Integer size;
    }

    /**
     * 系统消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMessageBody extends MessageBody implements Serializable {
        @NotBlank(message = "系统消息不能为空")
        private String text;
    }

    /**
     * 群组邀请消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupInviteMessageBody extends MessageBody implements Serializable {

        @NotBlank(message = "邀请ID不能为空")
        private String requestId;

        @NotBlank(message = "群组ID不能为空")
        private String groupId;

        @NotBlank(message = "群组名称不能为空")
        private String groupName;

        @NotBlank(message = "群组头像不能为空")
        private String groupAvatar;

        private String inviterId;

        private String inviterName;

        @NotBlank(message = "被邀请人不能为空")
        private String userId;

        private String userName;

        @NotNull(message = "邀请状态不能为空")
        // 1:待处理 2:已同意 3:已拒绝
        private Integer approveStatus;
    }

    /**
     * 位置消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationMessageBody extends MessageBody implements Serializable {
        @NotBlank(message = "位置标题不能为空")
        private String title;

        @NotBlank(message = "位置地址不能为空")
        private String address;

        private Double latitude;

        private Double longitude;
    }

    /**
     * 混合消息体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexMessageBody extends MessageBody implements Serializable {

        /**
         * 可选统一的 parts：数组的顺序即为消息内容的展示顺序
         * 每个 Part.type 可为 "text"|"image"|"video"|"audio"|"file"|"location"|"invite" 等
         */
        @NotNull(message = "parts 不能为空，至少传空列表")
        private List<Part> parts = Collections.emptyList();

        // 兼容字段（可选）：保留图片/视频列表供快速索引或旧代码使用
        private List<ImageMessageBody> images = Collections.emptyList();
        private List<VideoMessageBody> videos = Collections.emptyList();

        // 嵌套 Part 类
        @Getter
        @Setter
        @ToString
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Part implements Serializable {
            /**
             * 内容类型
             */
            @NotBlank
            private String type; // e.g. "text", "image", "video", "location", "file"

            /**
             * 若文本类型，则放在 content.text；否则 content 可为对象（图片/视频信息）
             */
            private Map<String, Object> content;

            /**
             * 可选元数据（例如 width/height/duration/alt 等）
             */
            private Map<String, Object> meta;
        }
    }

    /**
     * 撤回消息体（ Recall ）
     * 用于通知其他端某条消息被撤回
     * 约定：messageContentType == 11 表示此体
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecallMessageBody extends MessageBody implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 被撤回的 messageId（必填）
         */
        @NotBlank(message = "被撤回的 messageId 不能为空")
        private String messageId;

        /**
         * 操作者 ID（通常为原发送者，也可能为群主/管理员）
         */
        @NotBlank(message = "操作人 ID 不能为空")
        private String operatorId;

        /**
         * 撤回原因（可选）
         */
        private String reason;

        /**
         * 撤回时间（毫秒）
         */
        @NotNull(message = "撤回时间不能为空")
        private Long recallTime;

        /**
         * 可选：所属会话 id（toId 或 groupId），便于路由/展示
         */
        private String chatId;

        /**
         * 可选：会话类型（0 单聊 1 群聊 等）
         */
        private Integer chatType;
    }

    /**
     * 编辑/更新消息体（ Edit ）
     * 用于把一条消息的内容替换为新内容（编辑）
     * 约定：messageContentType == 12 表示此体
     * <p>
     * 注意：newMessageBody 使用 Map 以兼容任意 messageBody 结构（text/image/file 等）
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditMessageBody extends MessageBody implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 被编辑的 messageId（必填）
         */
        @NotBlank(message = "被编辑的 messageId 不能为空")
        private String messageId;

        /**
         * 编辑者 ID（通常为原发送者）
         */
        @NotBlank(message = "编辑者 ID 不能为空")
        private String editorId;

        /**
         * 编辑时间（毫秒）
         */
        @NotNull(message = "编辑时间不能为空")
        private Long editTime;

        /**
         * 新的 messageContentType（例如 1=text,2=image...），便于 Jackson 在消费端解析 newMessageBody
         */
        private Integer newMessageContentType;

        /**
         * 新的消息体（Map 结构以兼容任意 body 类型）
         * 后端在写 DB 时可以把它序列化为 JSON 存到 message_body 字段
         */
        @NotNull(message = "newMessageBody 不能为空")
        private Map<String, Object> newMessageBody;

        /**
         * 可选：编辑前的预览文本
         */
        private String oldPreview;

        /**
         * 可选：所属会话 id（toId 或 groupId），便于路由/展示
         */
        private String chatId;

        /**
         * 可选：会话类型（0 单聊 1 群聊 等）
         */
        private Integer chatType;
    }
}
