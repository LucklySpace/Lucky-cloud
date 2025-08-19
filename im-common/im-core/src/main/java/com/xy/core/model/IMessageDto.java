package com.xy.core.model;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.xy.core.enums.IMessageReadStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 抽象消息 DTO，定义通用属性
 *
 * @author dense
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class IMessageDto implements Serializable {

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
     * 额外信息
     */
    private String extra;

    /**
     * 被引用的消息 ID
     */
    private String replyTo;


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
            @JsonSubTypes.Type(value = SystemMessageBody.class, name = "10") // 系统消息
    })
    private MessageBody messageBody;


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
    public static class TextMessageBody extends MessageBody {

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
    public static class ImageMessageBody extends MessageBody {

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
    public static class VideoMessageBody extends MessageBody {

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
    public static class AudioMessageBody extends MessageBody {

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
    public static class FileMessageBody extends MessageBody {

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
    public static class SystemMessageBody extends MessageBody {

        @NotBlank(message = "系统消息不能为空")
        private String text;

    }

    /**
     * 复杂消息体，支持文本、图片、视频的组合
     */
    @Getter
    @Setter
    @ToString(callSuper = true)
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplexMessageBody extends MessageBody {

        /**
         * 可选文本文字
         */
        private String text;

        /**
         * 可选图片列表
         */
        @NotNull(message = "图片列表不能为空，请传空列表以表示无图片")
        private List<ImageMessageBody> images = Collections.emptyList();

        /**
         * 可选视频列表
         */
        @NotNull(message = "视频列表不能为空，请传空列表以表示无视频")
        private List<VideoMessageBody> videos = Collections.emptyList();
    }
}
