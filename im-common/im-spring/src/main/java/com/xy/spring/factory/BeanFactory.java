package com.xy.spring.factory;


/**
 * @author heyunlin
 * @version 1.0
 */
public interface BeanFactory<T> {

//    /**
//     * bean 对象池
//     */
//     Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
//
//    /**
//     * 单例对象池
//     */
//     Map<String, Object> singletonObjectMap = new ConcurrentHashMap<>();


    Object getBean(String beanName);

    T getBean(Class<?> type);

    T getBean(String beanName, Class<T> type);
}