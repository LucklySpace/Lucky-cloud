package com.xy.spring.aop;


import java.lang.reflect.Proxy;

public class AopProxyFactory {

    public static Object createProxy(Object target, Advisor advisor) {
        Class<?> targetClass = target.getClass();
        if (targetClass.getInterfaces().length > 0) {
            // 使用JDK代理
            return Proxy.newProxyInstance(
                    targetClass.getClassLoader(),
                    targetClass.getInterfaces(),
                    (proxy, method, args) -> advisor.invoke(method, args, target)
            );
        }

//        else {
//            // 使用CGLIB代理
//            Enhancer enhancer = new Enhancer();
//            enhancer.setSuperclass(targetClass);
//            enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> advisor.invoke(method, args, target));
//            return enhancer.create();
//        }
        return null;
    }
}

