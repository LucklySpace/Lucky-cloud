package com.xy.lucky.spring.boot.env;

/**
 * PropertyResolver - 属性解析器接口
 */
public interface PropertyResolver {

    /**
     * 判断是否包含指定的属性
     *
     * @param key 属性 key
     * @return 是否包含
     */
    boolean containsProperty(String key);

    /**
     * 获取属性值
     *
     * @param key 属性 key
     * @return 属性值，不存在则返回 null
     */
    String getProperty(String key);

    /**
     * 获取属性值，不存在则返回默认值
     *
     * @param key          属性 key
     * @param defaultValue 默认值
     * @return 属性值
     */
    String getProperty(String key, String defaultValue);

    /**
     * 获取属性值并转换为指定类型
     *
     * @param key        属性 key
     * @param targetType 目标类型
     * @param <T>        类型参数
     * @return 转换后的值，不存在则返回 null
     */
    <T> T getProperty(String key, Class<T> targetType);

    /**
     * 获取属性值并转换为指定类型，不存在则返回默认值
     *
     * @param key          属性 key
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @param <T>          类型参数
     * @return 转换后的值
     */
    <T> T getProperty(String key, Class<T> targetType, T defaultValue);

    /**
     * 获取必需的属性值，不存在则抛出异常
     *
     * @param key 属性 key
     * @return 属性值
     * @throws IllegalStateException 如果属性不存在
     */
    String getRequiredProperty(String key) throws IllegalStateException;

    /**
     * 获取必需的属性值并转换为指定类型，不存在则抛出异常
     *
     * @param key        属性 key
     * @param targetType 目标类型
     * @param <T>        类型参数
     * @return 转换后的值
     * @throws IllegalStateException 如果属性不存在
     */
    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

    /**
     * 解析占位符
     *
     * @param text 包含占位符的文本，如 ${app.name}
     * @return 解析后的文本
     */
    String resolvePlaceholders(String text);

    /**
     * 解析必需的占位符，如果有无法解析的占位符则抛出异常
     *
     * @param text 包含占位符的文本
     * @return 解析后的文本
     * @throws IllegalArgumentException 如果有无法解析的占位符
     */
    String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;
}

