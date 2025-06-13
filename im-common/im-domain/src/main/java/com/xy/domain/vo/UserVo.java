package com.xy.domain.vo;

import lombok.Data;

/**
 * @TableName im_user_data
 */
@Data
public class UserVo {

    private String userId;

    private String name;

    private String avatar;

    private Integer gender;

    private String birthDay;

    private String location;

    private String selfSignature;
}