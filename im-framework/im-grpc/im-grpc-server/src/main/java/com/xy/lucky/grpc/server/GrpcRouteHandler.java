package com.xy.lucky.grpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * gRPC Route Handler, wraps the actual method and object that handles requests
 * gRPC路由处理器，包装实际处理请求的方法和对象
 */
public class GrpcRouteHandler {
    private static final Logger log = LoggerFactory.getLogger(GrpcRouteHandler.class);

    // Bean object containing the handler method / 包含处理方法的Bean对象
    private final Object bean;
    // Handler method / 处理方法
    private final Method method;
    // Parameter types / 参数类型
    private final Class<?>[] paramTypes;
    // Return type / 返回类型
    private final Class<?> returnType;

    /**
     * Construct a route handler
     * 构造一个路由处理器
     *
     * @param bean   Bean object containing the handler method / 处理方法所在的bean对象
     * @param method Handler method / 处理方法
     */
    public GrpcRouteHandler(Object bean, Method method) {
        Objects.requireNonNull(bean, "bean cannot be null");
        // bean cannot be null
        Objects.requireNonNull(method, "method cannot be null");
        // method cannot be null

        this.bean = bean;
        this.method = method;
        Class<?>[] parameterTypes = method.getParameterTypes();
        this.paramTypes = (parameterTypes.length > 0) ? parameterTypes : null;
        this.returnType = method.getReturnType();

        log.debug("Create GrpcRouteHandler: bean={}, method={}, paramType={}, returnType={}",
                bean.getClass().getName(), method.getName(), this.paramTypes, this.returnType.getSimpleName());
        // 创建GrpcRouteHandler
    }

    /**
     * Get the bean object
     * 获取Bean对象
     *
     * @return Bean object / Bean对象
     */
    public Object getBean() {
        return bean;
    }

    /**
     * Get the handler method
     * 获取处理方法
     *
     * @return Handler method / 处理方法
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Get parameter types
     * 获取参数类型
     *
     * @return Parameter types / 参数类型
     */
    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    /**
     * Get return type
     * 获取返回类型
     *
     * @return Return type / 返回类型
     */
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrpcRouteHandler that = (GrpcRouteHandler) o;
        return Objects.equals(bean, that.bean) &&
                Objects.equals(method, that.method) &&
                Objects.equals(paramTypes, that.paramTypes) &&
                Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean, method, paramTypes, returnType);
    }

    @Override
    public String toString() {
        return "GrpcRouteHandler{" +
                "bean=" + bean.getClass().getSimpleName() +
                ", method=" + method.getName() +
                ", paramType=" + getParamTypes().toString() +
                ", returnType=" + returnType.getSimpleName() +
                '}';
    }
}