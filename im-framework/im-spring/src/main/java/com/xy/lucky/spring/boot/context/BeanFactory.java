package com.xy.lucky.spring.boot.context;

import java.util.Map;

/**
 * BeanFactory - Bean 工厂接口
 */
public interface BeanFactory {

    /**
     * 根据名称获取 Bean
     *
     * @param name Bean 名称
     * @return Bean 实例
     * @throws NoSuchBeanDefinitionException 如果 Bean 不存在
     */
    Object getBean(String name);

    /**
     * 根据名称和类型获取 Bean
     *
     * @param name         Bean 名称
     * @param requiredType 期望的类型
     * @param <T>          类型参数
     * @return Bean 实例
     * @throws NoSuchBeanDefinitionException  如果 Bean 不存在
     * @throws BeanNotOfRequiredTypeException 如果 Bean 类型不匹配
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 根据类型获取 Bean
     *
     * @param requiredType 期望的类型
     * @param <T>          类型参数
     * @return Bean 实例
     * @throws NoSuchBeanDefinitionException   如果 Bean 不存在
     * @throws NoUniqueBeanDefinitionException 如果存在多个匹配的 Bean
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 根据名称和构造参数获取 Bean
     *
     * @param name Bean 名称
     * @param args 构造参数
     * @return Bean 实例
     */
    Object getBean(String name, Object... args);

    /**
     * 根据类型和构造参数获取 Bean
     *
     * @param requiredType 期望的类型
     * @param args         构造参数
     * @param <T>          类型参数
     * @return Bean 实例
     */
    <T> T getBean(Class<T> requiredType, Object... args);

    /**
     * 根据类型获取所有匹配的 Bean
     *
     * @param type Bean 类型
     * @param <T>  类型参数
     * @return Bean 名称到实例的映射
     */
    <T> Map<String, T> getBeansOfType(Class<T> type);

    /**
     * 判断是否包含指定名称的 Bean 定义
     *
     * @param name Bean 名称
     * @return 是否包含
     */
    boolean containsBeanDefinition(String name);
}

