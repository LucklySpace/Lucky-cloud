package com.xy.lucky.spring.aop;

import com.xy.lucky.spring.annotations.aop.Around;
import com.xy.lucky.spring.annotations.aop.Aspect;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.context.ApplicationContext;
import com.xy.lucky.spring.core.ProxyType;
import com.xy.lucky.spring.factory.BeanProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP BeanPostProcessor：基于 @Aspect + @Around(execution(...)) 为目标 bean 创建代理。
 * <p>
 * 约束（保持实现简洁且更适合二进制/原生场景）：
 * - 默认只启用 JDK 代理（目标类需要实现接口），否则不代理；
 * - 若显式声明 ProxyType.BYTEBUDDY，则尝试使用 ByteBuddy（运行时字节码生成对原生镜像不友好，建议谨慎）。
 * - @Around 方法签名必须为：单参数 MethodInvocation，并返回 Object。
 * </p>
 */
public class ProxyBeanPostProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyBeanPostProcessor.class);

    private final List<Advisor> advisors = new ArrayList<>();
    private final ConcurrentHashMap<String, Object> earlyProxyReferences = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    @Autowired(name = "applicationContext", required = false)
    private ApplicationContext<?> applicationContext;

    public ProxyBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName, ProxyType proxyType) {
        ensureInitialized();
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName, ProxyType proxyType) {
        ensureInitialized();

        Object earlyProxy = earlyProxyReferences.remove(beanName);
        if (earlyProxy != null) return earlyProxy;

        if (bean instanceof BeanPostProcessor) return bean;
        if (bean.getClass().isAnnotationPresent(Aspect.class)) return bean;

        ProxyType effectiveType = resolveProxyType(bean, proxyType);
        if (effectiveType == ProxyType.NONE) return bean;

        List<Advisor> matched = matchAdvisors(bean.getClass());
        if (matched.isEmpty()) return bean;

        return createProxy(bean, beanName, matched, effectiveType);
    }

    @Override
    public Object getEarlyBeanReference(Object early, String beanName, ProxyType proxyType) {
        ensureInitialized();

        Object existing = earlyProxyReferences.get(beanName);
        if (existing != null) return existing;

        if (early instanceof BeanPostProcessor) return early;
        if (early.getClass().isAnnotationPresent(Aspect.class)) return early;

        ProxyType effectiveType = resolveProxyType(early, proxyType);
        if (effectiveType == ProxyType.NONE) return early;

        List<Advisor> matched = matchAdvisors(early.getClass());
        if (matched.isEmpty()) return early;

        Object proxy = createProxy(early, beanName, matched, effectiveType);
        earlyProxyReferences.put(beanName, proxy);
        return proxy;
    }

    private void ensureInitialized() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            Set<Class<?>> classes = applicationContext != null ? applicationContext.getScannedClasses() : Collections.emptySet();
            buildAdvisors(classes);
            initialized = true;
        }
    }

    private void buildAdvisors(Set<Class<?>> allClasses) {
        advisors.clear();
        for (Class<?> clazz : allClasses) {
            if (!clazz.isAnnotationPresent(Aspect.class)) continue;
            String aspectBeanName = deriveBeanName(clazz);
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Around.class)) continue;
                if (method.getParameterCount() != 1 || !MethodInvocation.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    logger.warn("@Around 方法签名不合法: {}.{}，要求单参数 MethodInvocation", clazz.getName(), method.getName());
                    continue;
                }
                Around around = method.getAnnotation(Around.class);
                advisors.add(new Advisor(around.value(), aspectBeanName, () -> applicationContext != null ? applicationContext.getBean(aspectBeanName) : null, method));
            }
        }
    }

    private List<Advisor> matchAdvisors(Class<?> targetClass) {
        if (advisors.isEmpty()) return Collections.emptyList();
        List<Advisor> matched = new ArrayList<>();
        Method[] methods = targetClass.getMethods();
        for (Advisor advisor : advisors) {
            for (Method m : methods) {
                if (advisor.matches(targetClass, m)) {
                    matched.add(advisor);
                    break;
                }
            }
        }
        return matched;
    }

    private Object createProxy(Object target, String beanName, List<Advisor> matched, ProxyType proxyType) {
        InvocationHandler handler = new AopInvocationHandler(target, matched);
        try {
            return BeanProxyFactory.createProxy(target, handler, proxyType);
        } catch (Exception e) {
            logger.warn("创建 AOP 代理失败，回退为原始 bean: {}", beanName, e);
            return target;
        }
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

    private String deriveBeanName(Class<?> cls) {
        return Introspector.decapitalize(cls.getSimpleName());
    }

    private static final class AopInvocationHandler implements InvocationHandler {
        private final Object target;
        private final List<Advisor> advisors;

        private AopInvocationHandler(Object target, List<Advisor> advisors) {
            this.target = Objects.requireNonNull(target, "target");
            this.advisors = Objects.requireNonNull(advisors, "advisors");
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method targetMethod = resolveTargetMethod(method);
            List<Advisor> matched = new ArrayList<>();
            for (Advisor advisor : advisors) {
                if (advisor.matches(target.getClass(), targetMethod)) {
                    matched.add(advisor);
                }
            }

            MethodInvocation invocation = new MethodInvocation(targetMethod, args, target) {
                private int index = -1;

                @Override
                public Object proceed() throws Throwable {
                    index++;
                    if (index >= matched.size()) {
                        return super.proceed();
                    }
                    return matched.get(index).invoke(this);
                }
            };

            return invocation.proceed();
        }

        private Method resolveTargetMethod(Method interfaceMethod) {
            try {
                return target.getClass().getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
                return interfaceMethod;
            }
        }
    }
}
