package com.xy.lucky.spring.aop;

import java.lang.reflect.Method;

public class Advisor {
    private final String expression;
    private final Class<?> aspectClass;
    private final Method aspectMethod;

    public Advisor(String expression, Class<?> aspectClass, Method aspectMethod) {
        this.expression = expression;
        this.aspectClass = aspectClass;
        this.aspectMethod = aspectMethod;
    }

    public boolean matches(Object bean) {
        // 简化版，仅通过包名匹配
        String className = bean.getClass().getName();
        String pattern = expression.replace("execution(", "").replace(")", "")
                .replace("*", ".*").replace("..", ".*");
        return className.matches(pattern);
    }

    public Object invoke(Method method, Object[] args, Object target) throws Throwable {
        Object aspectInstance = aspectClass.getConstructor().newInstance();
        return aspectMethod.invoke(aspectInstance, new MethodInvocation(method, args, target));
    }
}
