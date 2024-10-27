package com.xy.auth.domain.vo;

import lombok.Data;

/**
 * @TableName im_user_data
 */
@Data
public class UserVo {

    private String user_id;

    private String name;

    private String avatar;

    private Integer user_sex;

    private String birth_day;

    private String location;

    private String self_signature;
}