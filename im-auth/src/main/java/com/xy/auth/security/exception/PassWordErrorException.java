package com.xy.auth.security.exception;

import org.springframework.security.core.AuthenticationException;

public class PassWordErrorException extends AuthenticationException {
    public PassWordErrorException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PassWordErrorException(String msg) {
        super(msg);
    }
}