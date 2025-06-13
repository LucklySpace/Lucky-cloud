package com.xy.connect.config;

import com.xy.spring.annotations.aop.Around;
import com.xy.spring.annotations.aop.Aspect;
import com.xy.spring.aop.MethodInvocation;

@Aspect
public class LogAspect {

    @Around("execution(com.xy.connect)")
    public Object log(MethodInvocation invocation) throws Throwable {
        System.out.println("[AOP LOG] Before: " + invocation.getMethod().getName());
        Object result = invocation.proceed();
        System.out.println("[AOP LOG] After: " + invocation.getMethod().getName());
        return result;
    }
}
