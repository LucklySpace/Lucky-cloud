package com.xy.lucky.connect.config;

import com.xy.lucky.spring.annotations.aop.Around;
import com.xy.lucky.spring.annotations.aop.Aspect;
import com.xy.lucky.spring.aop.MethodInvocation;

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
