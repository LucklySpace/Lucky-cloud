package com.xy.lucky.spring.boot.context;

import com.xy.lucky.spring.boot.env.ConfigurableEnvironment;

import java.util.Set;

/**
 * ConfigurableApplicationContext - 可配置的应用上下文接口
 */
public interface ConfigurableApplicationContext extends ApplicationContext, AutoCloseable {

    /**
     * 设置环境
     *
     * @param environment 环境
     */
    void setEnvironment(ConfigurableEnvironment environment);

    /**
     * 设置主配置源
     *
     * @param primarySources 主配置类集合
     */
    void setPrimarySources(Set<Class<?>> primarySources);

    /**
     * 设置启动参数
     *
     * @param args 命令行参数
     */
    void setArgs(String[] args);

    /**
     * 刷新上下文（触发 Bean 扫描和初始化）
     */
    void refresh();

    /**
     * 判断上下文是否处于活跃状态
     *
     * @return 是否活跃
     */
    boolean isActive();

    /**
     * 关闭上下文
     */
    @Override
    void close();

    /**
     * 执行所有 Runner
     *
     * @param args 命令行参数
     */
    void callRunners(String[] args);

    /**
     * 注册 ShutdownHook
     */
    void registerShutdownHook();
}

