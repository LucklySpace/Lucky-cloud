package com.xy.utils;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

/**
 * bean转map工具类
 */

public class BeanToMapUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * bean 转 map（深拷贝支持嵌套结构）
     */
    public static Map<String, Object> beanToMap(Object bean) {
        if (bean == null) return Collections.emptyMap();
        return objectMapper.convertValue(bean, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * map 转 bean（支持嵌套结构）
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanClass) {
        return objectMapper.convertValue(map, beanClass);
    }

    /**
     * map 转复杂类型对象（如包含 List、Map、泛型等）
     */
    public static <T> T mapToComplexType(Map<String, Object> map, TypeReference<T> typeRef) {
        return objectMapper.convertValue(map, typeRef);
    }
}