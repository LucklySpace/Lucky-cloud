package com.xy.lucky.spring.boot.env;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MutablePropertySources - 可变的属性源集合
 * <p>
 * 支持按优先级顺序存储多个属性源，先添加的优先级更高
 */
public class MutablePropertySources implements Iterable<PropertySource<?>> {

    private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<>();

    public MutablePropertySources() {
    }

    public MutablePropertySources(PropertySources propertySources) {
        for (PropertySource<?> propertySource : propertySources) {
            addLast(propertySource);
        }
    }

    @Override
    public Iterator<PropertySource<?>> iterator() {
        return this.propertySourceList.iterator();
    }

    /**
     * 判断是否包含指定名称的属性源
     */
    public boolean contains(String name) {
        return get(name) != null;
    }

    /**
     * 根据名称获取属性源
     */
    public PropertySource<?> get(String name) {
        for (PropertySource<?> propertySource : this.propertySourceList) {
            if (propertySource.getName().equals(name)) {
                return propertySource;
            }
        }
        return null;
    }

    /**
     * 添加到最前面（最高优先级）
     */
    public void addFirst(PropertySource<?> propertySource) {
        removeIfPresent(propertySource);
        this.propertySourceList.add(0, propertySource);
    }

    /**
     * 添加到最后面（最低优先级）
     */
    public void addLast(PropertySource<?> propertySource) {
        removeIfPresent(propertySource);
        this.propertySourceList.add(propertySource);
    }

    /**
     * 添加到指定属性源之前
     */
    public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        removeIfPresent(propertySource);
        int index = indexOf(relativePropertySourceName);
        if (index == -1) {
            throw new IllegalArgumentException("PropertySource '" + relativePropertySourceName + "' not found");
        }
        this.propertySourceList.add(index, propertySource);
    }

    /**
     * 添加到指定属性源之后
     */
    public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        removeIfPresent(propertySource);
        int index = indexOf(relativePropertySourceName);
        if (index == -1) {
            throw new IllegalArgumentException("PropertySource '" + relativePropertySourceName + "' not found");
        }
        this.propertySourceList.add(index + 1, propertySource);
    }

    /**
     * 获取属性源的索引位置
     */
    public int indexOf(String name) {
        for (int i = 0; i < this.propertySourceList.size(); i++) {
            if (this.propertySourceList.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 移除指定名称的属性源
     */
    public PropertySource<?> remove(String name) {
        int index = indexOf(name);
        if (index >= 0) {
            return this.propertySourceList.remove(index);
        }
        return null;
    }

    /**
     * 替换指定属性源
     */
    public void replace(String name, PropertySource<?> propertySource) {
        int index = indexOf(name);
        if (index >= 0) {
            this.propertySourceList.set(index, propertySource);
        }
    }

    /**
     * 获取属性源数量
     */
    public int size() {
        return this.propertySourceList.size();
    }

    private void removeIfPresent(PropertySource<?> propertySource) {
        this.propertySourceList.removeIf(ps -> ps.getName().equals(propertySource.getName()));
    }

    private void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
        String newName = propertySource.getName();
        if (relativePropertySourceName.equals(newName)) {
            throw new IllegalArgumentException("PropertySource '" + newName + "' cannot be relative to itself");
        }
    }

    @Override
    public String toString() {
        return this.propertySourceList.toString();
    }
}

