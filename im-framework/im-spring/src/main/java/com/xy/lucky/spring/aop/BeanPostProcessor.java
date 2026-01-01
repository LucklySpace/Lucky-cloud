package com.xy.lucky.spring.aop;

import com.xy.lucky.spring.core.ProxyType;
import com.xy.lucky.spring.exception.BeansException;

/**
 * BeanPostProcessor（bean的后处理器）是Spring中非常重要的接口，其中定义的两个方法会在每个bean的初始化前和初始化后被调用。
 */
public interface BeanPostProcessor {

    /**
     * 在 Bean 对象执行初始化方法之前，执行此方法
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName, ProxyType proxyType) throws BeansException {
        return bean;
    }

    /**
     * 在 Bean 对象执行初始化方法之后，执行此方法
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 初始化后处理（带 ProxyType）。默认回退到旧签名以保持兼容。
     */
    default Object postProcessAfterInitialization(Object bean, String beanName, ProxyType proxyType) throws BeansException {
        return postProcessAfterInitialization(bean, beanName);
    }

    /**
     * 早期引用（用于解决单例循环依赖的提前暴露/提前代理）。
     * 默认不做处理，直接返回 early，避免返回 null 破坏创建流程。
     */
    default Object getEarlyBeanReference(Object early, String beanName) {
        return early;
    }

    /**
     * 早期引用（带 ProxyType）。默认回退到不带 ProxyType 的实现。
     */
    default Object getEarlyBeanReference(Object early, String beanName, ProxyType proxyType) {
        return getEarlyBeanReference(early, beanName);
    }
}


