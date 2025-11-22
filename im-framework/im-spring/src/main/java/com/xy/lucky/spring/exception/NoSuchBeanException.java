package com.xy.lucky.spring.exception;

public class NoSuchBeanException extends RuntimeException {

    public NoSuchBeanException() {
    }


    public NoSuchBeanException(String message) {
        super(message);
    }
}
