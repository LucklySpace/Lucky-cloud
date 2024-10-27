package com.xy.server.exception;

import com.xy.server.response.ResultEnum;

public class ForbiddenException extends RuntimeException {

    private int code;

    private String message;

    public ForbiddenException(ResultEnum resultEnum) {
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