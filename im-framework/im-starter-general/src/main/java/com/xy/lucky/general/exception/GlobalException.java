package com.xy.lucky.general.exception;

import com.xy.lucky.general.response.domain.ResultCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GlobalException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;

    public GlobalException(String message) {
        this.code = ResultCode.FAIL.getCode();
        this.message = message;
    }

    public GlobalException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public GlobalException(ResultCode resultEnum) {
        this.code = resultEnum.getCode();
        this.message = resultEnum.getMessage();
    }

    public GlobalException(ResultCode resultEnum, String message) {
        this.code = resultEnum.getCode();
        this.message = message;
    }

}