package com.xy.lucky.chat.exception.handler;

import com.xy.lucky.general.exception.BusinessException;
import com.xy.lucky.general.exception.ForbiddenException;
import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.nio.file.AccessDeniedException;
import java.rmi.ServerException;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * @author dense
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.xy.lucky")
@Order(Ordered.HIGHEST_PRECEDENCE)// 设置最高优先级
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handle(BusinessException ex) {
        log.error("BusinessException: {}", ex.getMessage(), ex);
        return Result.failed(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理缺失必填参数异常
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Result<?> handle(WebExchangeBindException ex) {
        log.error("WebExchangeBindException: {}", ex.getMessage(), ex);
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, msg);
    }

    /**
     * 处理 PathVariable / RequestParam 校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handle(ConstraintViolationException ex) {
        log.error("Constraint Violation: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, ex.getMessage());
    }

    /**
     * 处理禁止访问异常
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result<?> handle(ForbiddenException ex) {
        log.error("ForbiddenException: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.FORBIDDEN);
    }

    /**
     * 处理访问权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handle(AccessDeniedException ex) {
        log.warn("AccessDeniedException: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.NO_PERMISSION);
    }

    /**
     * 处理大文件上传异常
     */
    @ExceptionHandler(DataBufferLimitException.class)
    public Result<?> handle(DataBufferLimitException ex) {
        log.error("DataBufferLimitException: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.REQUEST_DATA_TOO_LARGE);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handle(NullPointerException ex) {
        log.error("NullPointerException: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.NOT_FOUND);
    }

    /**
     * 处理服务异常
     */
    @ExceptionHandler(ServerException.class)
    public Result<?> handle(ServerException ex) {
        log.error("ServerException: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.SERVICE_EXCEPTION);
    }

    /**
     * 统一异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception ex) {
        log.error("Unhandled Exception: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
