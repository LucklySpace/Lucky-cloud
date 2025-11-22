package com.xy.lucky.spring.aop;

import com.xy.lucky.spring.XSpringApplication;
import com.xy.lucky.spring.annotations.core.Async;
import com.xy.lucky.spring.core.ProxyType;
import com.xy.lucky.spring.exception.BeansException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

/**
 * 异步方法处理器
 */
public class AsyncBeanPostProcessor implements BeanPostProcessor {

    private final Executor taskExecutor;

    public AsyncBeanPostProcessor() {
        // 使用默认的线程池
        this.taskExecutor = XSpringApplication.getContext().getTaskExecutor();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName, ProxyType proxyType) throws BeansException {
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName, proxyType);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 检查是否有@Async注解的方法
        boolean hasAsyncMethods = false;
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Async.class)) {
                hasAsyncMethods = true;
                break;
            }
        }

        // 如果有@Async注解的方法，则创建代理
        if (hasAsyncMethods) {
            return Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    bean.getClass().getInterfaces(),
                    new AsyncInvocationHandler(bean, taskExecutor)
            );
        }

        return bean;
    }

    @Override
    public Object getEarlyBeanReference(Object early, String name) {
        return null;
    }

    /**
     * 异步方法调用处理器
     */
    private static class AsyncInvocationHandler implements InvocationHandler {
        private final Object target;
        private final Executor taskExecutor;

        public AsyncInvocationHandler(Object target, Executor taskExecutor) {
            this.target = target;
            this.taskExecutor = taskExecutor;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 检查方法是否有@Async注解
            if (method.isAnnotationPresent(Async.class)) {
                // 异步执行方法
                taskExecutor.execute(() -> {
                    try {
                        method.invoke(target, args);
                    } catch (Exception e) {
                        // 记录异常，实际项目中可能需要更完善的异常处理机制
                        e.printStackTrace();
                    }
                });
                // 异步方法不返回结果
                return null;
            } else {
                // 同步执行方法
                return method.invoke(target, args);
            }
        }
    }
}