package com.xy.connect.exception;

import java.io.Serializable;

/**
 * netty 异常
 */
public class NettyException extends RuntimeException implements Serializable {

    public NettyException(String message) {
        super(message);
    }

    public NettyException(String message, Throwable cause) {
        super(message, cause);
    }

}