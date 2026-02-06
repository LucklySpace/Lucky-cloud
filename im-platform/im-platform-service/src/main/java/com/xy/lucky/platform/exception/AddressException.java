package com.xy.lucky.platform.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 地址异常
 */
public class AddressException extends GlobalException {

    public AddressException(String message) {
        super(message);
    }

    public AddressException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
