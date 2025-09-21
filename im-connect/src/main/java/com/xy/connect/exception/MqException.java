package com.xy.connect.exception;

import java.io.Serializable;


/**
 * mq异常
 */
public class MqException extends RuntimeException implements Serializable {

    public MqException(String message) {
        super(message);
    }

    public MqException(String message, Throwable cause) {
        super(message, cause);
    }

}
