package com.xy.general.version.core;

import com.xy.general.version.annotations.Version;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * 自定义 HandlerMapping，支持基于 @Version 注解的接口版本控制
 * <p>
 * 示例用法：
 * - 类或方法上加 @Version("v1") 表示匹配 /v1/xxx 请求
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
        return null == version ? super.getCustomTypeCondition(handlerType) : new ApiVersionCondition(version.value());
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
        return null == version ? super.getCustomMethodCondition(method) : new ApiVersionCondition(version.value());
    }


}