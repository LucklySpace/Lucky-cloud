package com.xy.auth.security.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @description:用户名和密码验证的token
 */
public class UserAuthenticationToken extends UsernamePasswordAuthenticationToken {


    public UserAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

}

