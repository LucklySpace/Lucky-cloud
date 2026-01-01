package com.xy.lucky.platform.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

public class NotifyException extends GlobalException {

    public NotifyException(String message) {
        super(message);
    }

    public NotifyException(ResultCode resultEnum) {
        super(resultEnum);
    }
}
