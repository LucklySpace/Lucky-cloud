package com.xy.connect.exception.handler;

import com.xy.spring.exception.CyclicDependencyException;
import com.xy.spring.exception.NoSuchBeanException;
import com.xy.spring.exception.handler.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义异常处理器
 */
@Slf4j
public class GlobalExceptionHandler implements ExceptionHandler {

    @Override
    public void handle(Exception ex, String context) {
        if (ex instanceof NoSuchBeanException) {
            // 记录并快速退出，不做报警
            log.warn("{}: {}", context, ex.getMessage());
        } else if (ex instanceof CyclicDependencyException) {
            // 重要异常：上报并可能触发熔断
            //monitoringClient.reportCritical(context, ex);
        } else {
            // 默认行为
            log.error("Unhandled exception in {}: ", context, ex);
            //monitoringClient.reportError(context, ex);
        }
    }
}
