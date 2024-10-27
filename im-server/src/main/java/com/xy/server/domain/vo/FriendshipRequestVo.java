package com.xy.server.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Data
@Accessors(chain = true)
public class FriendshipRequestVo implements Serializable {

    private String id;

    private String from_id;

    private String to_id;

    private String name;

    private String avatar;

    private String add_wording;

    private Integer approve_status;

}