package com.xy.lucky.platform.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 表情包业务异常
 */
public class EmojiException extends GlobalException {

    public EmojiException(String message) {
        super(message);
    }

    public EmojiException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
