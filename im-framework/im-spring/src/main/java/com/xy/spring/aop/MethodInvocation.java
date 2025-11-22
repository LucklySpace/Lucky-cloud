package com.xy.spring.aop;

import java.lang.reflect.Method;

public class MethodInvocation {

    private final Method method;
    private final Object[] args;
    private final Object target;

    public MethodInvocation(Method method, Object[] args, Object target) {
        this.method = method;
        this.args = args;
        this.target = target;
    }

    public Object proceed() throws Throwable {
        return method.invoke(target, args);
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
}
