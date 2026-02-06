package com.xy.lucky.platform.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 版本异常
 */
public class ReleaseException extends GlobalException {

    public ReleaseException(String message) {
        super(message);
    }

    public ReleaseException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
