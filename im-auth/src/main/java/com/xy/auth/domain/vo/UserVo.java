package com.xy.auth.domain.vo;

import lombok.Data;

/**
 * @TableName im_user_data
 */
@Data
public class UserVo {

    private String userId;

    private String name;

    private String avatar;

    private Integer userSex;

    private String birthDay;

    private String location;

    private String selfSignature;
}