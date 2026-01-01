package com.xy.lucky.spring.aop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class MethodInvocation {

    private final Method method;
    private final Object[] args;
    private final Object target;
    private final MethodHandle targetInvoker;

    public MethodInvocation(Method method, Object[] args, Object target) {
        this.method = method;
        this.args = args;
        this.target = target;
        this.targetInvoker = resolveInvoker(method, target);
    }

    private static MethodHandle resolveInvoker(Method method, Object target) {
        try {
            Class<?> targetClass = target.getClass();
            MethodType type = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            return MethodHandles.lookup()
                    .findVirtual(targetClass, method.getName(), type)
                    .bindTo(target);
        } catch (Exception e) {
            throw new IllegalStateException("解析方法句柄失败: " + method.getName(), e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getTarget() {
        return target;
    }

    public Object proceed() throws Throwable {
        if (args == null || args.length == 0) {
            return targetInvoker.invoke();
        }
        return targetInvoker.invokeWithArguments(args);
    }
}
