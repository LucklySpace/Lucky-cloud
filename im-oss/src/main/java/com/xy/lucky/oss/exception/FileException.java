package com.xy.lucky.oss.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 文件异常
 */
public class FileException extends GlobalException {

    public FileException(String message) {
        super(message);
    }

    public FileException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
