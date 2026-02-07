package com.xy.lucky.chat.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 会话异常
 */
public class ChatException extends GlobalException {

    public ChatException(String message) {
        super(message);
    }

    public ChatException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
