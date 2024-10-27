package com.xy.server.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatVo<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private String chat_id;
    private Integer chat_type;
    private String owner_id;
    private String to_id;
    private Integer is_mute;
    private Integer is_top;
    private Long sequence;
    private String name;
    private String avatar;
    private Integer unread;
    private String id; // group_id or user_id
    private Object message;
    private Long message_time;

}
