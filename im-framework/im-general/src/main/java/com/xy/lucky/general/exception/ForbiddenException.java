package com.xy.lucky.general.exception;

import com.xy.lucky.general.response.domain.ResultCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 403 Forbidden
 */
@Data
public class ForbiddenException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;

    private String message;

    public ForbiddenException(ResultCode resultEnum) {
        this.code = resultEnum.getCode();
        this.message = resultEnum.getMessage();
    }
}