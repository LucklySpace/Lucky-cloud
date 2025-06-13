package com.xy.spring.aop;

import com.xy.spring.core.ProxyType;
import com.xy.spring.exception.BeansException;

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

}


