package com.xy.lucky.lbs.exception.handler;


import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.lbs.exception.ResponseNotIntercept;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
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
@RestControllerAdvice(basePackages = "com.xy.lucky.lbs")
@Order(Ordered.HIGHEST_PRECEDENCE)// 设置最高优先级
public class GlobalExceptionHandler implements ResponseBodyAdvice<Object> {


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
        // "请求数据大小不允许超过10MB"
        return Result.failed(ResultCode.REQUEST_DATA_TOO_LARGE);
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
        // 资源未找到
        return Result.failed(ResultCode.NOT_FOUND);
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
        // 服务异常
        return Result.failed(ResultCode.SERVICE_EXCEPTION);
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneralException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        // 服务器内部异常，请稍后重试
        return Result.failed(ResultCode.INTERNAL_SERVER_ERROR);
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
