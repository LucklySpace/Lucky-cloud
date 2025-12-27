package com.xy.lucky.general.version.core;

import com.xy.lucky.general.version.annotations.Version;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * 自定义 HandlerMapping，支持基于 @Version 注解的接口版本控制
 *
 * 示例用法：
 * - 类或方法上加 @Version("1") 表示匹配 /v1/xxx 请求
 */
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    /**
     * 获取类上的自定义条件（即 @Version 注解）
     *
     * @param handlerType 控制器类
     * @return 请求条件（用于路由匹配）
     */
    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        // 查找类上的 @Version 注解
        Version version = AnnotationUtils.findAnnotation(handlerType, Version.class);
        // 如果类上没有@Version注解，则使用父类的实现
        return version == null ? super.getCustomTypeCondition(handlerType) : new ApiVersionCondition(version.value());
    }

    /**
     * 获取方法上的自定义条件（即 @Version 注解）
     *
     * @param method 控制器方法
     * @return 请求条件（用于路由匹配）
     */
    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        // 查找方法上的 @Version 注解
        Version version = AnnotationUtils.findAnnotation(method, Version.class);
        // 如果方法上没有@Version注解，则使用父类的实现
        return version == null ? super.getCustomMethodCondition(method) : new ApiVersionCondition(version.value());
    }
}
