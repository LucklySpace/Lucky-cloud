package com.xy.lucky.chat.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendshipRequestVo implements Serializable {

    private String id;

    private String fromId;

    private String toId;

    private String name;

    private String avatar;

    private String message;

    private Long createTime;

    private Integer approveStatus;

}