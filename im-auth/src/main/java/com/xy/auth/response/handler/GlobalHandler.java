package com.xy.auth.response.handler;


import com.xy.auth.response.ResponseNotIntercept;
import com.xy.auth.response.Result;
import com.xy.auth.security.exception.AuthenticationFailException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.naming.SizeLimitExceededException;
import java.rmi.ServerException;

/**
 * 全局异常处理
 *
 * @author dense
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.xy.auth")
public class GlobalHandler implements ResponseBodyAdvice<Object> {

    /**
     * 自定义认证异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication error: {}", ex.getMessage(), ex);

        if (ex instanceof AuthenticationFailException e) {
            return Result.failed(e.getResultCode().getCode(), e.getResultCode().getZh());
        }

        return Result.failed(1000, ex.getMessage());
    }

    /**
     * 处理Validated验证异常
     *
     * @param e
     * @return
     */
    @ExceptionHandler({BindException.class})
    public Result<?> bindExceptionHandler(BindException e) {
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        log.error("BindException：", e);
        return Result.failed(objectError.getDefaultMessage());
    }


    /**
     * 处理请求数据超大异常
     *
     * @param e
     * @return
     * @ExceptionHandler
     */
    @ExceptionHandler(SizeLimitExceededException.class)
    public Result<?> sizeLimitExceededExceptionHandler(SizeLimitExceededException e) {
        log.error("SizeLimitExceededException异常：", e);
        return Result.failed("请求数据大小不允许超过10MB");
    }


    /**
     * 空值异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException ex) {
        // 对空指针异常的处理逻辑
        log.error("Authentication error: {}", ex.getMessage(), ex);

        return Result.failed("result is null ");
    }


    /**
     * 服务异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(ServerException.class)
    public Result<?> handleServerException(ServerException ex) {
        log.error("Server error: {}", ex.getMessage(), ex);
        return Result.failed(6000, ex.getMessage());
    }

    /**
     * 权限不足
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage(), ex);
        return Result.failed(403, "权限不足");
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneralException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Result.failed(500, "服务器内部异常，请稍后重试");
    }


    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 判断方法或类上是否存在 @ResponseNotIntercept 注解
        if (returnType.getDeclaringClass().isAnnotationPresent(ResponseNotIntercept.class) ||
                returnType.getMethod().isAnnotationPresent(ResponseNotIntercept.class)) {
            return false;
        }
        return true;
    }

    /**
     * https://www.cnblogs.com/oldboyooxx/p/10824531.html
     * string 返回值 序列化异常
     *
     * @return
     */
    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            return body;
        }
        return Result.success(body);
    }

}
