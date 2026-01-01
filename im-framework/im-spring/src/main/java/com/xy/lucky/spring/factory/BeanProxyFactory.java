package com.xy.lucky.spring.factory;

import com.xy.lucky.spring.core.ProxyType;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

/**
 * 代理工厂，支持 JDK 动态代理和 Byte Buddy 代理（替代 CGLIB）
 */
public class BeanProxyFactory {


    /**
     * 创建代理对象
     *
     * @param target    目标对象
     * @param handler   方法拦截器（必须实现 InvocationHandler）
     * @param proxyType 代理类型（JDK, BYTEBUDDY 或 AUTO）
     * @param <T>       目标类型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, InvocationHandler handler, ProxyType proxyType) {
        if (target == null) throw new IllegalArgumentException("Target cannot be null");
        if (handler == null) throw new IllegalArgumentException("Handler cannot be null");
        if (proxyType == null) throw new IllegalArgumentException("ProxyType cannot be null");

        ProxyType type = proxyType == ProxyType.AUTO ? resolveType(target) : proxyType;
        switch (type) {
            case JDK:
                return createJdkProxy(target, handler);
            case BYTEBUDDY:
                return createByteBuddyProxy(target, handler);
            default:
                throw new IllegalStateException("Unsupported proxy type: " + type);
        }
    }

    /**
     * 根据目标类是否实现接口判断使用 JDK 或 Byte Buddy
     */
    private static <T> ProxyType resolveType(T target) {
        return target.getClass().getInterfaces().length > 0 ? ProxyType.JDK : ProxyType.BYTEBUDDY;
    }

    /**
     * 创建 JDK 动态代理
     */
    @SuppressWarnings("unchecked")
    private static <T> T createJdkProxy(T target, InvocationHandler handler) {
        Class<?>[] interfaces = target.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalStateException("Target class must implement at least one interface for JDK proxy");
        }
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                interfaces,
                handler
        );
    }

    /**
     * 创建 Byte Buddy 代理
     */
    @SuppressWarnings("unchecked")
    private static <T> T createByteBuddyProxy(T target, InvocationHandler handler) {
        try {
            Class<? extends T> dynamicType = new ByteBuddy()
                    .subclass((Class<T>) target.getClass())
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(new ByteBuddyInterceptor<>(target, handler)))
                    .make()
                    .load(target.getClass().getClassLoader(), net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();

            MethodHandle ctor = MethodHandles.lookup().findConstructor(dynamicType, MethodType.methodType(void.class));
            return (T) ctor.invoke();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Byte Buddy proxy for " + target.getClass().getName(), e);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate Byte Buddy proxy for " + target.getClass().getName(), t);
        }
    }

    /**
     * Byte Buddy 方法拦截器：将方法调用委托给 InvocationHandler
     */
    public static class ByteBuddyInterceptor<T> {
        private final T target;
        private final InvocationHandler handler;

        public ByteBuddyInterceptor(T target, InvocationHandler handler) {
            this.target = target;
            this.handler = handler;
        }

        @RuntimeType
        public Object intercept(
                @Origin Method method,
                @AllArguments Object[] args,
                @SuperCall Callable<?> superCall) throws Throwable {
            return handler.invoke(target, method, args);
        }
    }

    /**
     * 示例拦截器：日志记录
     */
    public static class LoggingInterceptor implements InvocationHandler {
        private final Object target;

        public LoggingInterceptor(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("Before method: " + method.getName());
            MethodHandle handle = MethodHandles.lookup()
                    .findVirtual(target.getClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()))
                    .bindTo(target);
            Object result = (args == null || args.length == 0) ? handle.invoke() : handle.invokeWithArguments(args);
            System.out.println("After method: " + method.getName());
            return result;
        }
    }
}
