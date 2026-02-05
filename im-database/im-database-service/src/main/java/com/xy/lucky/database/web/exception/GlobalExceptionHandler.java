package com.xy.lucky.database.web.exception;

import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException ex) {
        log.error("Missing Parameter: {}", ex.getMessage());
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    String name = path.substring(path.lastIndexOf('.') + 1);
                    return name + ": " + v.getMessage();
                })
                .collect(java.util.stream.Collectors.joining(", "));
        log.error("Constraint Violation: {}", msg);
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, msg);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Type Mismatch: {}", ex.getMessage());
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, "参数: " + ex.getName() + " 类型错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));
        log.error("MethodArgumentNotValid: {}", msg);
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBind(BindException ex) {
        String msg = ex.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(java.util.stream.Collectors.joining("; "));
        log.error("BindException: {}", msg);
        return Result.failed(ResultCode.VALIDATION_INCOMPLETE, msg);
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
