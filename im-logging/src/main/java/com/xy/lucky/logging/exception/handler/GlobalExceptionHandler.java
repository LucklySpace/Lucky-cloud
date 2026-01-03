package com.xy.lucky.logging.exception.handler;


import com.xy.lucky.general.exception.BusinessException;
import com.xy.lucky.general.exception.ForbiddenException;
import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.logging.exception.LoggingException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.naming.SizeLimitExceededException;
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
     * 处理日志异常
     */
    @ExceptionHandler(LoggingException.class)
    public Result<?> handle(LoggingException ex) {
        log.error("LoggingException: {}", ex.getMessage(), ex);
        return Result.failed(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理缺失必填参数异常
     */
//    @ExceptionHandler(MissingServletRequestParameterException.class)
//    public Result<?> handle(MissingServletRequestParameterException ex) {
//        log.error("Missing Parameter: {}", ex.getMessage(), ex);
//        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, ex.getMessage());
//    }

    /**
     * 处理 PathVariable / RequestParam 校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handle(ConstraintViolationException ex) {
        log.error("Constraint Violation: {}", ex.getMessage(), ex);

        String msg = ex.getConstraintViolations().stream()
                .map(violation -> {
                    // 获取属性路径的最后一个节点作为参数名
                    String propertyPath = violation.getPropertyPath().toString();
                    String paramName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
                    return paramName + ": " + violation.getMessage();
                })
                .collect(Collectors.joining(", "));

        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, msg);
    }

    /**
     * 处理参数类型不符异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handle(MethodArgumentTypeMismatchException ex) {
        log.error("Type Mismatch: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, "参数: " + ex.getName() + " 类型错误");
    }

    /**
     * 处理输入体校验异常 (@RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handle(MethodArgumentNotValidException ex) {
        log.error("MethodArgumentNotValid: {}", ex.getMessage(), ex);
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, msg);
    }

    /**
     * 处理 form-data 校验异常 (@ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handle(BindException ex) {
        log.error("BindException: {}", ex.getMessage(), ex);
        String msg = ex.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, msg);
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
    @ExceptionHandler(SizeLimitExceededException.class)
    public Result<?> handle(SizeLimitExceededException ex) {
        log.error("SizeLimitExceededException: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.REQUEST_DATA_TOO_LARGE);
    }

    /**
     * 处理上传大小超限异常（Spring Multipart）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handle(MaxUploadSizeExceededException ex) {
        log.error("MaxUploadSizeExceededException: {}", ex.getMessage(), ex);
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
