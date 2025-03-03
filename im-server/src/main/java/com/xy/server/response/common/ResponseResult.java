package com.xy.server.response.common;


import com.xy.server.response.ResponseNotIntercept;
import com.xy.server.response.Result;
import com.xy.server.utils.JsonUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;


/**
 * 统一封装controller层返回的结果
 */
@RestControllerAdvice(basePackages = "com.xy.server")
public class ResponseResult implements ResponseBodyAdvice<Object> {


    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 判断方法或类上是否存在 @ResponseNotIntercept 注解
        if (returnType.getDeclaringClass().isAnnotationPresent(ResponseNotIntercept.class) ||
                returnType.getMethod().isAnnotationPresent(ResponseNotIntercept.class)) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            // 提供一定的灵活度，如果body已经被包装了，就不进行包装
            return body;
        }
        if (body instanceof String) {
            //解决返回值为字符串时，不能正常包装
            return JsonUtil.toJSONString(Result.success(body));
        }
        return Result.success(body);
    }
}
