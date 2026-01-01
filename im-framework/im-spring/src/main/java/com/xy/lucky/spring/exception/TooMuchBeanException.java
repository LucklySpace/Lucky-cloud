package com.xy.lucky.spring.exception;

public class TooMuchBeanException extends RuntimeException {

    public TooMuchBeanException() {
    }

    public TooMuchBeanException(String message) {
        super(message);
    }
}
