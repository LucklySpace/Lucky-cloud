package com.xy.lucky.server.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 消息异常
 */
public class MessageException extends GlobalException {

    public MessageException(String message) {
        super(message);
    }

    public MessageException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
