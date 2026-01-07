package com.xy.lucky.spring.boot.context;

import com.xy.lucky.spring.boot.env.Environment;

/**
 * ApplicationContext - 应用上下文接口
 */
public interface ApplicationContext extends BeanFactory {

    /**
     * 获取应用 ID
     *
     * @return 应用 ID
     */
    String getId();

    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    String getApplicationName();

    /**
     * 获取显示名称
     *
     * @return 显示名称
     */
    String getDisplayName();

    /**
     * 获取启动时间戳
     *
     * @return 启动时间（毫秒）
     */
    long getStartupDate();

    /**
     * 获取父上下文
     *
     * @return 父上下文，没有则返回 null
     */
    ApplicationContext getParent();

    /**
     * 获取环境
     *
     * @return 环境
     */
    Environment getEnvironment();

    /**
     * 判断是否包含指定名称的 Bean
     *
     * @param name Bean 名称
     * @return 是否包含
     */
    boolean containsBean(String name);

    /**
     * 判断指定 Bean 是否为单例
     *
     * @param name Bean 名称
     * @return 是否单例
     */
    boolean isSingleton(String name);

    /**
     * 判断指定 Bean 是否为原型
     *
     * @param name Bean 名称
     * @return 是否原型
     */
    boolean isPrototype(String name);

    /**
     * 获取指定 Bean 的类型
     *
     * @param name Bean 名称
     * @return Bean 类型，不存在返回 null
     */
    Class<?> getType(String name);

    /**
     * 获取所有 Bean 定义的名称
     *
     * @return Bean 名称数组
     */
    String[] getBeanDefinitionNames();

    /**
     * 获取 Bean 定义数量
     *
     * @return 数量
     */
    int getBeanDefinitionCount();
}

