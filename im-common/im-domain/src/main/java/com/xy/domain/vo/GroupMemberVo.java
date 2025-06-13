package com.xy.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberVo {

    private String userId;

    private String name;

    private String avatar;

    private Integer gender;

    private String birthDay;

    private String location;

    private String selfSignature;

    private Integer mute;

    private String alias;

    private Integer role;

    private String joinType;

}
