package com.xy.imcore.model;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author dense
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class IMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发送者
     */
    @NotBlank(message = "发送人id不能为空")
    private String fromId;

    /**
     * 临时消息id
     */
    @NotBlank(message = "消息临时id不能为空")
    private String messageTempId;

    /**
     * 消息id
     */
    private String messageId;

    /**
     * 消息内容
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageContentType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextMessageBody.class, name = "1"), // 文本消息
            @JsonSubTypes.Type(value = ImageMessageBody.class, name = "2"),// 图片消息
            @JsonSubTypes.Type(value = VideoMessageBody.class, name = "3"),// 视频消息

            @JsonSubTypes.Type(value = SystemMessageBody.class, name = "10") // 系统消息
    })
    private MessageBody messageBody;

    /**
     * 消息内容类型
     */
    private Integer messageContentType;

    /**
     * 消息时间戳
     */
    private Long messageTime;

    /**
     * 消息阅读状态
     */
    private Integer readStatus;

    private Long sequence;

    private String extra;


    // 基类
    @Getter
    @Setter
    public static abstract class MessageBody {
        // 公共字段
    }

    // 文本消息类
    @Getter
    @Setter
    @ToString
    public static class TextMessageBody extends MessageBody {

        @NotBlank(message = "消息内容不能为空")
        private String message;
    }

    // 图片消息类
    @Getter
    @Setter
    @ToString
    public static class ImageMessageBody extends MessageBody {
        private String name;
        private String url;
        private Integer size;
    }

    // 视频消息类
    @Getter
    @Setter
    @ToString
    public static class VideoMessageBody extends MessageBody {
        private String url;
    }

    // 系统消息类
    @Getter
    @Setter
    @ToString
    public static class SystemMessageBody extends MessageBody {
        private String message;
    }

}
