package com.xy.server.domain.vo;


import lombok.Data;


@Data
public class FriendVo {

    private String userId;

    private String friendId;

    private String name;

    private String alias; // 别名

    private String avatar;

    private Integer gender;

    private String location;

    private Integer black; // 是否拉黑 1正常 2拉黑

    private Integer flag;

    private String birthDay;

    private String selfSignature;

    private Long sequence;

}
