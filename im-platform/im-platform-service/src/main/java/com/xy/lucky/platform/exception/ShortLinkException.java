package com.xy.lucky.platform.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 短链异常
 */
public class ShortLinkException extends GlobalException {

    public ShortLinkException(String message) {
        super(message);
    }

    public ShortLinkException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
