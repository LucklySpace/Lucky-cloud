package com.xy.lucky.spring.exception.handler;


import com.xy.lucky.spring.exception.CyclicDependencyException;
import com.xy.lucky.spring.exception.NoSuchBeanException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局默认异常处理器，实现基础日志分级与分发
 */
@Slf4j
public class GlobalExceptionHandler implements ExceptionHandler {

    @Override
    public void handle(Exception e, String context) {
        if (e instanceof NoSuchBeanException) {
            log.error("[BeanNotFound] {} – {}", context, e.getMessage());
        } else if (e instanceof CyclicDependencyException) {
            log.error("[CyclicDependency] {} – {}", context, e.getMessage(), e);
        } else {
            log.error("[UnhandledException] {} – {}", context, e.getMessage(), e);
        }
    }
}