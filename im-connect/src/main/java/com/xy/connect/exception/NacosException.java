package com.xy.connect.exception;

import java.io.Serializable;

/**
 * nacos 异常
 */
public class NacosException extends RuntimeException implements Serializable {

    public NacosException(String message) {
        super(message);
    }

    public NacosException(String message, Throwable cause) {
        super(message, cause);
    }

}