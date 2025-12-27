package com.xy.lucky.domain.vo;

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
public class UserVo {

    private String userId;

    private String name;

    private String avatar;

    private Integer gender;

    private String birthday;

    private String location;

    private String selfSignature;
}