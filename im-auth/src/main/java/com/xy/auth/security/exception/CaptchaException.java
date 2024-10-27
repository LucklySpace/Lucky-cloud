package com.xy.auth.security.exception;

import org.springframework.security.core.AuthenticationException;


/**
 * 验证码错误的异常类。
 */
public class CaptchaException extends AuthenticationException {
    public CaptchaException(String msg, Throwable t) {
        super(msg, t);
    }

    public CaptchaException(String msg) {
        super(msg);
    }
}
