package com.xy.lucky.utils.object;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Object 工具类
 *
 */
public class ObjectUtils {

    /**
     * 复制对象，并忽略 Id 编号
     *
     * @param object 被复制对象
     * @param consumer 消费者，可以二次编辑被复制对象
     * @return 复制后的对象
     */
    public static <T> T cloneIgnoreId(T object, Consumer<T> consumer) {
        T result = clone(object);
        // 忽略 id 编号
        Field field = getField(object.getClass(), "id");
        if (field != null) {
            setFieldValue(result, field, null);
        }
        // 二次编辑
        consumer.accept(result);
        return result;
    }

    /**
     * 获取类中的指定字段
     *
     * @param clazz     类
     * @param fieldName 字段名
     * @return 字段对象，如果找不到返回null
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 设置对象字段的值
     *
     * @param object 对象
     * @param field  字段
     * @param value  值
     */
    public static void setFieldValue(Object object, Field field, Object value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("无法设置字段值: " + field.getName(), e);
        }
    }

    /**
     * 克隆对象（简单实现，实际项目中可能需要更复杂的克隆逻辑）
     *
     * @param object 要克隆的对象
     * @return 克隆后的对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T object) {
        // 这里只是一个简单的示例实现，实际项目中可能需要使用序列化或其他方式实现深拷贝
        return object;
    }

    public static <T extends Comparable<T>> T max(T obj1, T obj2) {
        if (obj1 == null) {
            return obj2;
        }
        if (obj2 == null) {
            return obj1;
        }
        return obj1.compareTo(obj2) > 0 ? obj1 : obj2;
    }

    @SafeVarargs
    public static <T> T defaultIfNull(T... array) {
        for (T item : array) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    @SafeVarargs
    public static <T> boolean equalsAny(T obj, T... array) {
        return Arrays.asList(array).contains(obj);
    }

}