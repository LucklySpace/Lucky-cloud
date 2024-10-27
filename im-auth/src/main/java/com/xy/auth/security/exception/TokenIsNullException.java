package com.xy.auth.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * token为空或不符合规范的异常类。
 */
public class TokenIsNullException extends AuthenticationException {
    public TokenIsNullException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TokenIsNullException(String msg) {
        super(msg);
    }
}
