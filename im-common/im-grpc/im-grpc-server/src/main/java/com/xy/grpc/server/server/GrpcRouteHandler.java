package com.xy.grpc.server.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * gRPC路由处理器，包装实际处理请求的方法和对象
 */
public class GrpcRouteHandler {
    private static final Logger log = LoggerFactory.getLogger(GrpcRouteHandler.class);

    private final Object bean;
    private final Method method;
    private final Class<?> paramType;
    private final Class<?> returnType;

    /**
     * 构造一个路由处理器
     *
     * @param bean   处理方法所在的bean对象
     * @param method 处理方法
     */
    public GrpcRouteHandler(Object bean, Method method) {
        Objects.requireNonNull(bean, "bean不能为空");
        Objects.requireNonNull(method, "method不能为空");

        this.bean = bean;
        this.method = method;
        Class<?>[] parameterTypes = method.getParameterTypes();
        this.paramType = (parameterTypes.length > 0) ? parameterTypes[0] : Void.class;
        this.returnType = method.getReturnType();

        log.debug("Create GrpcRouteHandler: bean={}, method={}, paramType={}, returnType={}",
                bean.getClass().getName(), method.getName(), this.paramType.getSimpleName(), this.returnType.getSimpleName());
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getParamType() {
        return paramType;
    }

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
                Objects.equals(paramType, that.paramType) &&
                Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean, method, paramType, returnType);
    }

    @Override
    public String toString() {
        return "GrpcRouteHandler{" +
                "bean=" + bean.getClass().getSimpleName() +
                ", method=" + method.getName() +
                ", paramType=" + paramType.getSimpleName() +
                ", returnType=" + returnType.getSimpleName() +
                '}';
    }
}