package com.xy.auth.domain;


import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    // 用户名、手机号或二维码字符串
    private String principal;

    // 加密后的密码、验证码或二维码登录密码
    private String credentials;

    // 认证类型，例如 "form", "sms", "scan"
    private String authType;


}