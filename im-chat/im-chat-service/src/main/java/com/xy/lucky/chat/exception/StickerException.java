package com.xy.lucky.chat.exception;

import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 表情包异常
 */
public class StickerException extends GlobalException {

    public StickerException(String message) {
        super(message);
    }

    public StickerException(ResultCode resultEnum) {
        super(resultEnum);
    }

}
