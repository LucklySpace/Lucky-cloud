package com.xy.lucky.database.exception;

import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 数据库服务全局异常处理器
 * 处理数据库服务中的各种异常，统一返回格式
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.xy.lucky.database")
public class GlobalExceptionHandler {

    /**
     * 处理访问权限异常
     *
     * @param ex ForbiddenException异常实例
     * @return 统一响应结果
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result<?> handleForbiddenException(ForbiddenException ex) {
        log.warn("ForbiddenException: {}", ex.getMessage());
        return Result.failed(ResultCode.FORBIDDEN);
    }

    /**
     * 处理通用异常
     *
     * @param ex Exception异常实例
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneralException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.INTERNAL_SERVER_ERROR);
    }
}