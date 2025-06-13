package com.xy.domain.vo;


import lombok.Data;


@Data
public class FriendVo {

    private String userId;

    private String friendId;

    private String name;

    // 别名
    private String alias;

    private String avatar;

    private Integer gender;

    private String location;

    // 是否拉黑 1正常 2拉黑
    private Integer black;

    private Integer flag;

    private String birthDay;

    private String selfSignature;

    private Long sequence;

}
