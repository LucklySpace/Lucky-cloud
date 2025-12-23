package com.xy.lucky.platform.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 更新异常
 */
public class UpdateException extends GlobalException {

    public UpdateException(String message) {
        super(message);
    }

    public UpdateException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
