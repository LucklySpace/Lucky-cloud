package com.xy.lucky.security.exception;

import com.xy.lucky.general.response.domain.ResultCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

import java.io.Serial;

@Getter
public class AuthenticationFailException extends AuthenticationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ResultCode resultCode;

    public AuthenticationFailException(String msg, Throwable cause) {
        super(msg, cause);
        this.resultCode = null;
    }

    public AuthenticationFailException(String msg) {
        super(msg);
        this.resultCode = null;
    }

    public AuthenticationFailException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public AuthenticationFailException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
    }
}

