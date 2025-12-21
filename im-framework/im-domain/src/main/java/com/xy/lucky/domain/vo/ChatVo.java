package com.xy.lucky.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String chatId;

    private Integer chatType;

    private String ownerId;

    private String toId;

    private Integer isMute;

    private Integer isTop;

    private Long sequence;

    private String name;

    private String avatar;

    private Integer unread;

    // groupId or userId
    private String id;

    private Object message;

    private Integer messageContentType;

    private Long messageTime;

}
