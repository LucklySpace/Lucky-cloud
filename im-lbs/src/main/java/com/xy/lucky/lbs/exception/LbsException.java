package com.xy.lucky.lbs.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 地址异常
 */
public class LbsException extends GlobalException {

    public LbsException(String message) {
        super(message);
    }

    public LbsException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
