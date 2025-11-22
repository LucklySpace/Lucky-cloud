package com.xy.lucky.spring.exception;


/**
 * 自定义异常类，用于表示循环依赖
 */
public class CyclicDependencyException extends RuntimeException {
    public CyclicDependencyException(String message) {
        super(message);
    }
}