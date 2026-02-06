package com.xy.lucky.platform.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 语言包异常
 */
public class LanguagePackException extends GlobalException {
    public LanguagePackException(String message) {
        super(message);
    }

    public LanguagePackException(ResultCode resultEnum) {
        super(resultEnum);
    }
}
