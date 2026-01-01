package com.xy.lucky.spring.aop;

import com.xy.lucky.spring.annotations.core.Async;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.core.ProxyType;
import com.xy.lucky.spring.factory.BeanProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 异步方法处理器
 */
public class AsyncBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncBeanPostProcessor.class);
    private final ConcurrentHashMap<String, Object> earlyProxyReferences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Boolean> asyncMethodCache = new ConcurrentHashMap<>();
    @Autowired(name = "taskExecutor", required = false)
    private Executor taskExecutor;

    public AsyncBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName, ProxyType proxyType) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName, ProxyType proxyType) {
        Object earlyProxy = earlyProxyReferences.remove(beanName);
        if (earlyProxy != null) return earlyProxy;

        if (!hasAsyncMethod(bean.getClass())) return bean;

        ProxyType effectiveType = resolveProxyType(bean, proxyType);
        if (effectiveType == ProxyType.NONE) return bean;

        Object proxy = BeanProxyFactory.createProxy(bean, new AsyncInvocationHandler(bean, getExecutor()), effectiveType);
        return proxy;
    }

    @Override
    public Object getEarlyBeanReference(Object early, String beanName, ProxyType proxyType) {
        Object existing = earlyProxyReferences.get(beanName);
        if (existing != null) return existing;

        if (!hasAsyncMethod(early.getClass())) return early;

        ProxyType effectiveType = resolveProxyType(early, proxyType);
        if (effectiveType == ProxyType.NONE) return early;

        Object proxy = BeanProxyFactory.createProxy(early, new AsyncInvocationHandler(early, getExecutor()), effectiveType);
        earlyProxyReferences.put(beanName, proxy);
        return proxy;
    }

    private Executor getExecutor() {
        Executor executor = this.taskExecutor;
        return executor != null ? executor : ForkJoinPool.commonPool();
    }

    private boolean hasAsyncMethod(Class<?> targetClass) {
        return asyncMethodCache.computeIfAbsent(targetClass, cls -> {
            for (Method m : cls.getMethods()) {
                if (m.isAnnotationPresent(Async.class)) return true;
            }
            return false;
        });
    }

    private ProxyType resolveProxyType(Object bean, ProxyType configured) {
        ProxyType type = configured == null ? ProxyType.NONE : configured;
        if (type == ProxyType.AUTO) {
            return bean.getClass().getInterfaces().length > 0 ? ProxyType.JDK : ProxyType.NONE;
        }
        if (type == ProxyType.JDK && bean.getClass().getInterfaces().length == 0) {
            return ProxyType.NONE;
        }
        return type;
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

        private static Method resolveTargetMethod(Method interfaceMethod, Object target) {
            try {
                return target.getClass().getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
                return interfaceMethod;
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method targetMethod = resolveTargetMethod(method, target);
            MethodHandle invoker = MethodHandles.lookup()
                    .findVirtual(target.getClass(), targetMethod.getName(),
                            MethodType.methodType(targetMethod.getReturnType(), targetMethod.getParameterTypes()))
                    .bindTo(target);
            if (targetMethod.isAnnotationPresent(Async.class)) {
                taskExecutor.execute(() -> {
                    try {
                        if (args == null || args.length == 0) {
                            invoker.invoke();
                        } else {
                            invoker.invokeWithArguments(args);
                        }
                    } catch (Exception e) {
                        log.error("异步方法执行异常: {}.{}", target.getClass().getName(), targetMethod.getName(), e);
                    } catch (Throwable t) {
                        log.error("异步方法执行异常: {}.{}", target.getClass().getName(), targetMethod.getName(), t);
                    }
                });
                return null;
            } else {
                if (args == null || args.length == 0) {
                    return invoker.invoke();
                }
                return invoker.invokeWithArguments(args);
            }
        }
    }
}
