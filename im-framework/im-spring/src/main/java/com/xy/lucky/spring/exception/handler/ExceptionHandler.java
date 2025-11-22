package com.xy.lucky.spring.exception.handler;

/**
 * 异常处理接口，框架中所有异常可统一分发到实现类
 */
public interface ExceptionHandler {
    void handle(Exception e, String context);
}
