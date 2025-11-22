package com.xy.spring.exception;

public class NoSuchBeanException extends RuntimeException {

    public NoSuchBeanException() {
    }


    public NoSuchBeanException(String message) {
        super(message);
    }
}
