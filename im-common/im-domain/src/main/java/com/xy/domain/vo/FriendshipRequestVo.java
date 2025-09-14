package com.xy.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Data
@Accessors(chain = true)
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