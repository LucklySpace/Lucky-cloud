package com.xy.server.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberVo {

    private String user_id;

    private String name;

    private String avatar;

    private Integer user_sex;

    private String birth_day;

    private String location;

    private String self_signature;

    private Integer mute;

    private String alias;

    private Integer role;

    private String join_type;

}
