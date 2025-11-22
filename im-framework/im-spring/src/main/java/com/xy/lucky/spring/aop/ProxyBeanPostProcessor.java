package com.xy.lucky.spring.aop;


import com.xy.lucky.spring.annotations.aop.Around;
import com.xy.lucky.spring.annotations.aop.Aspect;
import com.xy.lucky.spring.core.ProxyType;
import com.xy.lucky.spring.factory.BeanProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// ProxyBeanPostProcessor.java
public class ProxyBeanPostProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyBeanPostProcessor.class);

    private final List<Advisor> advisors = new ArrayList<>();

    public ProxyBeanPostProcessor(Set<Class<?>> allClasses) {
        for (Class<?> clazz : allClasses) {
            if (clazz.isAnnotationPresent(Aspect.class)) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Around.class)) {
                        Around around = method.getAnnotation(Around.class);
                        advisors.add(new Advisor(around.value(), clazz, method));
                    }
                }
            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName, ProxyType proxyType) {
        logger.debug("为 bean {} 创建代理", beanName);
        if (proxyType == ProxyType.NONE) return bean;
        return BeanProxyFactory.createProxy(bean, new SimpleLoggingInterceptor(), proxyType);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        for (Advisor advisor : advisors) {
            if (advisor.matches(bean)) {
                return AopProxyFactory.createProxy(bean, advisor);
            }
        }
        return bean;
    }

    @Override
    public Object getEarlyBeanReference(Object early, String name) {
        return null;
    }

    /**
     * 示例拦截器：记录方法调用日志
     */
    private static class SimpleLoggingInterceptor implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            logger.info("调用方法: {}.{}", proxy.getClass().getSimpleName(), method.getName());
            Object result = method.invoke(proxy, args);
            logger.info("方法 {} 执行完成，返回: {}", method.getName(), result);
            return result;
        }
    }

}
