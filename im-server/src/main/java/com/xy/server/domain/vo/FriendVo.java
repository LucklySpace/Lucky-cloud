package com.xy.server.domain.vo;


import lombok.Data;


@Data
public class FriendVo {

    private String user_id;

    private String friend_id;

    private String name;

    private String alias; // 别名

    private String avatar;

    private Integer user_sex;

    private String location;

    private Integer black; // 是否拉黑 1正常 2拉黑

    private Integer flag;

    private String birth_day;

    private String self_signature;

    private Long sequence;

}
