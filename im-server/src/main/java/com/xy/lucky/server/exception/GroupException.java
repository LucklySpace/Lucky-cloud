package com.xy.lucky.server.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 群组异常
 */
public class GroupException extends GlobalException {

    public GroupException(String message) {
        super(message);
    }

    public GroupException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
