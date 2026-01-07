package com.xy.lucky.spring.boot.env;

import java.util.Map;

/**
 * ConfigurableEnvironment - 可配置的环境接口
 */
public interface ConfigurableEnvironment extends Environment {

    /**
     * 设置激活的 profiles
     *
     * @param profiles profile 名称数组
     */
    void setActiveProfiles(String... profiles);

    /**
     * 添加激活的 profile
     *
     * @param profile profile 名称
     */
    void addActiveProfile(String profile);

    /**
     * 设置默认的 profiles
     *
     * @param profiles profile 名称数组
     */
    void setDefaultProfiles(String... profiles);

    /**
     * 获取属性源集合
     *
     * @return PropertySources
     */
    MutablePropertySources getPropertySources();

    /**
     * 获取系统属性
     *
     * @return 系统属性 Map
     */
    Map<String, Object> getSystemProperties();

    /**
     * 获取系统环境变量
     *
     * @return 环境变量 Map
     */
    Map<String, Object> getSystemEnvironment();

    /**
     * 合并父环境的配置
     *
     * @param parent 父环境
     */
    void merge(ConfigurableEnvironment parent);

    /**
     * 解析命令行参数
     *
     * @param args 命令行参数
     */
    void parseCommandLineArgs(String[] args);

    /**
     * 设置属性
     *
     * @param key   属性 key
     * @param value 属性值
     */
    void setProperty(String key, String value);
}

