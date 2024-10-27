package com.xy.imcore.model;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class IMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * from user
     */
    private String from_id;

    /**
     * message id
     */
    private String message_id;

    /**
     * message content
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "message_content_type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextMessageBody.class, name = "1"), // 文本消息
            @JsonSubTypes.Type(value = ImageMessageBody.class, name = "2"),// 图片消息
            @JsonSubTypes.Type(value = VideoMessageBody.class, name = "3"),// 视频消息

            @JsonSubTypes.Type(value = SystemMessageBody.class, name = "10") // 系统消息
    })
    private MessageBody message_body;

    /**
     * message content type
     */
    private String message_content_type;

    /**
     * message timestamp
     */
    private Long message_time;

    /**
     * message read status
     */
    private Integer read_status;

    private Long sequence;

    private Object extra;


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
