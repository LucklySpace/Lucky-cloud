package com.xy.lucky.database.exception;

import com.xy.lucky.general.response.domain.ResultCode;

/**
 * 自定义禁止访问异常类
 * 用于处理内部调用权限验证失败的情况
 */
public class ForbiddenException extends RuntimeException {

    private final int code;
    private final String message;

    public ForbiddenException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public ForbiddenException(String message) {
        super(message);
        this.code = ResultCode.FORBIDDEN.getCode();
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}