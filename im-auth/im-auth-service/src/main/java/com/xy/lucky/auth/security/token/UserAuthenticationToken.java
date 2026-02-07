package com.xy.lucky.auth.security.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * 账号密码登录
 */
public class UserAuthenticationToken extends UsernamePasswordAuthenticationToken {


    public UserAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

}

