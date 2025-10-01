package com.xy.domain.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    // 是否好友 1正常 2非好友
    private Integer flag;

    private String birthDay;

    private String selfSignature;

    private Long sequence;

}
