package com.xy.lucky.ai.exception.handler;

import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.xy.lucky.ai")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handle(MissingServletRequestParameterException ex) {
        log.error("Missing Parameter: {}", ex.getMessage());
        return Result.failed(ResultCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handle(MethodArgumentTypeMismatchException ex) {
        log.error("Type Mismatch: {}", ex.getMessage());
        return Result.failed(ResultCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handle(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("Validation Error: {}", msg);
        return Result.failed(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handle(BindException ex) {
        String msg = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("Bind Error: {}", msg);
        return Result.failed(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(NullPointerException.class)
    public Result<?> handle(NullPointerException ex) {
        log.error("NullPointerException: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception ex) {
        log.error("Unhandled Exception: {}", ex.getMessage(), ex);
        return Result.failed(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
