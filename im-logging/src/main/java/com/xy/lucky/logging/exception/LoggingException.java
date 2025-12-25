package com.xy.lucky.logging.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 日志异常
 */
public class LoggingException extends GlobalException {

    public LoggingException(String message) {
        super(message);
    }

    public LoggingException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
