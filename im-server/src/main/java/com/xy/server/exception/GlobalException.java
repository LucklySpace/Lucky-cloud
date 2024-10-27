package com.xy.server.exception;

import com.xy.server.response.ResultEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class GlobalException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 8134030011662574394L;
    private Integer code;
    private String message;

    public GlobalException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public GlobalException(ResultEnum resultEnum) {
        this.code = resultEnum.getCode();
        this.message = resultEnum.getMessage();
    }


    public GlobalException(ResultEnum resultEnum, String message) {
        this.code = resultEnum.getCode();
        this.message = message;
    }


}