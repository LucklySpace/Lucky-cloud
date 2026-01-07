package com.xy.lucky.spring.boot.context;

import com.xy.lucky.spring.boot.annotation.ConfigurationProperties;
import com.xy.lucky.spring.boot.annotation.NestedConfigurationProperty;
import com.xy.lucky.spring.boot.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

/**
 * ConfigurationPropertiesBinder - 配置属性绑定器
 * <p>
 * 将 Environment 中的配置绑定到 @ConfigurationProperties 标注的 Bean 上
 */
public class ConfigurationPropertiesBinder {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationPropertiesBinder.class);

    private final Environment environment;

    public ConfigurationPropertiesBinder(Environment environment) {
        this.environment = environment;
    }

    /**
     * 绑定配置到目标对象
     *
     * @param target 目标对象
     */
    public void bind(Object target) {
        Class<?> targetClass = target.getClass();
        ConfigurationProperties annotation = targetClass.getAnnotation(ConfigurationProperties.class);
        if (annotation == null) {
            return;
        }

        String prefix = getPrefix(annotation);
        boolean ignoreInvalidFields = annotation.ignoreInvalidFields();

        try {
            bindProperties(target, targetClass, prefix, ignoreInvalidFields);
        } catch (Exception e) {
            log.error("Failed to bind configuration properties for {}", targetClass.getName(), e);
            if (!ignoreInvalidFields) {
                throw new RuntimeException("Configuration binding failed for " + targetClass.getName(), e);
            }
        }
    }

    /**
     * 获取配置前缀
     */
    private String getPrefix(ConfigurationProperties annotation) {
        String prefix = annotation.prefix();
        if (prefix.isEmpty()) {
            prefix = annotation.value();
        }
        return prefix;
    }

    /**
     * 绑定属性到对象
     */
    private void bindProperties(Object target, Class<?> targetClass, String prefix, boolean ignoreInvalidFields) {
        for (Field field : getAllFields(targetClass)) {
            try {
                bindField(target, field, prefix, ignoreInvalidFields);
            } catch (Exception e) {
                log.warn("Failed to bind field {}.{}: {}", targetClass.getSimpleName(), field.getName(), e.getMessage());
                if (!ignoreInvalidFields) {
                    throw new RuntimeException("Failed to bind field " + field.getName(), e);
                }
            }
        }
    }

    /**
     * 获取所有字段（包括父类）
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 绑定单个字段
     */
    private void bindField(Object target, Field field, String prefix, boolean ignoreInvalidFields) throws Exception {
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
            return;
        }

        // 设置字段可访问
        field.setAccessible(true);

        String propertyName = toPropertyName(field.getName());
        String fullKey = prefix.isEmpty() ? propertyName : prefix + "." + propertyName;

        // 检查是否为嵌套属性
        if (field.isAnnotationPresent(NestedConfigurationProperty.class) || isComplexType(field.getType())) {
            bindNestedProperty(target, field, fullKey, ignoreInvalidFields);
            return;
        }

        // 获取属性值
        Object value = getPropertyValue(fullKey, field.getType(), field.getGenericType());
        if (value != null) {
            field.set(target, value);
            log.debug("Bound property: {} = {}", fullKey, value);
        }
    }

    /**
     * 绑定嵌套属性
     */
    private void bindNestedProperty(Object target, Field field, String prefix, boolean ignoreInvalidFields) throws Exception {
        Class<?> fieldType = field.getType();

        // 确保字段可访问
        field.setAccessible(true);

        // 跳过集合和基本类型
        if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) {
            Object value = getPropertyValue(prefix, fieldType, field.getGenericType());
            if (value != null) {
                field.set(target, value);
            }
            return;
        }

        // 获取或创建嵌套对象实例
        Object nestedObject = field.get(target);
        if (nestedObject == null) {
            try {
                Constructor<?> constructor = fieldType.getDeclaredConstructor();
                constructor.setAccessible(true);
                nestedObject = constructor.newInstance();
                field.set(target, nestedObject);
            } catch (Exception e) {
                log.warn("Cannot instantiate nested object for field: {}", field.getName());
                return;
            }
        }

        // 递归绑定嵌套对象的属性
        bindProperties(nestedObject, fieldType, prefix, ignoreInvalidFields);
    }

    /**
     * 判断是否为复杂类型（需要嵌套绑定）
     */
    private boolean isComplexType(Class<?> type) {
        return !type.isPrimitive() &&
                !type.getName().startsWith("java.lang") &&
                !type.getName().startsWith("java.util") &&
                !type.isEnum() &&
                !type.isArray();
    }

    /**
     * 将驼峰命名转换为配置属性名（支持 kebab-case）
     */
    private String toPropertyName(String fieldName) {
        // 保持原名，支持驼峰
        return fieldName;
    }

    /**
     * 从环境中获取属性值并转换类型
     */
    @SuppressWarnings("unchecked")
    private Object getPropertyValue(String key, Class<?> targetType, Type genericType) {
        // 处理 List 类型
        if (List.class.isAssignableFrom(targetType)) {
            return getListValue(key, genericType);
        }

        // 处理 Set 类型
        if (Set.class.isAssignableFrom(targetType)) {
            List<?> list = getListValue(key, genericType);
            return list != null ? new LinkedHashSet<>(list) : null;
        }

        // 处理 Map 类型
        if (Map.class.isAssignableFrom(targetType)) {
            return getMapValue(key, genericType);
        }

        // 处理数组类型
        if (targetType.isArray()) {
            return getArrayValue(key, targetType.getComponentType());
        }

        // 处理基本类型和包装类型
        String value = environment.getProperty(key);
        if (value == null) {
            return null;
        }

        return convertValue(value, targetType);
    }

    /**
     * 获取 List 类型的值
     */
    @SuppressWarnings("unchecked")
    private List<?> getListValue(String key, Type genericType) {
        Class<?> elementType = Object.class;
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                elementType = (Class<?>) typeArgs[0];
            }
        }

        List<Object> result = new ArrayList<>();

        // 尝试索引方式获取 list[0], list[1], ...
        for (int i = 0; ; i++) {
            String indexedKey = key + "[" + i + "]";
            String value = environment.getProperty(indexedKey);
            if (value == null) {
                // 也尝试 key.0, key.1 格式
                indexedKey = key + "." + i;
                value = environment.getProperty(indexedKey);
            }
            if (value == null) {
                break;
            }
            result.add(convertValue(value, elementType));
        }

        // 如果索引方式没有值，尝试逗号分隔
        if (result.isEmpty()) {
            String value = environment.getProperty(key);
            if (value != null && !value.isEmpty()) {
                String[] parts = value.replace("[", "").replace("]", "").split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        result.add(convertValue(trimmed, elementType));
                    }
                }
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * 获取数组类型的值
     */
    private Object getArrayValue(String key, Class<?> componentType) {
        List<?> list = getListValue(key, null);
        if (list == null || list.isEmpty()) {
            return null;
        }

        Object array = Array.newInstance(componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    /**
     * 获取 Map 类型的值
     */
    private Map<String, Object> getMapValue(String key, Type genericType) {
        // 简化实现：暂不支持 Map 绑定
        return null;
    }

    /**
     * 类型转换
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        value = value.trim();

        if (targetType == String.class) {
            return value;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(value);
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.parseShort(value);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.parseByte(value);
        }
        if (targetType == Character.class || targetType == char.class) {
            return value.isEmpty() ? '\0' : value.charAt(0);
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, value);
        }

        return value;
    }
}
