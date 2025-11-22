package com.xy.lucky.server.exception;

import com.xy.lucky.general.response.domain.ResultCode;

public class BusinessException extends RuntimeException {

    private int code;

    private String message;

    public BusinessException(ResultCode resultEnum) {
        this.code = resultEnum.getCode();
        this.message = resultEnum.getMessage();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
