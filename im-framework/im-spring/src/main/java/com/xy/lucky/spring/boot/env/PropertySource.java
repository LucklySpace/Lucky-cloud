package com.xy.lucky.spring.boot.env;

import java.util.Objects;

/**
 * PropertySource - 属性源抽象类
 *
 * @param <T> 源数据类型
 */
public abstract class PropertySource<T> {

    protected final String name;
    protected final T source;

    public PropertySource(String name, T source) {
        Objects.requireNonNull(name, "Property source name must not be null");
        this.name = name;
        this.source = source;
    }

    public PropertySource(String name) {
        this(name, null);
    }

    /**
     * 获取属性源名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取底层数据源
     */
    public T getSource() {
        return this.source;
    }

    /**
     * 判断是否包含指定属性
     */
    public boolean containsProperty(String name) {
        return getProperty(name) != null;
    }

    /**
     * 获取属性值
     *
     * @param name 属性名
     * @return 属性值，不存在返回 null
     */
    public abstract Object getProperty(String name);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertySource<?> that = (PropertySource<?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name='" + name + "'}";
    }
}

