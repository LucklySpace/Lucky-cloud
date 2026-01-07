package com.xy.lucky.spring.boot.env;

/**
 * PropertySources - 属性源集合接口
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

    /**
     * 判断是否包含指定名称的属性源
     *
     * @param name 属性源名称
     * @return 是否包含
     */
    boolean contains(String name);

    /**
     * 根据名称获取属性源
     *
     * @param name 属性源名称
     * @return 属性源，不存在返回 null
     */
    PropertySource<?> get(String name);
}

