package com.xy.auth.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * token验证失败
 */
public class TokenIsInvalidException extends AuthenticationException {
    public TokenIsInvalidException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TokenIsInvalidException(String msg) {
        super(msg);
    }
}
