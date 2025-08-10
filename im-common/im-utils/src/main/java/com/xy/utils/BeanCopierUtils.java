package com.xy.utils;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.core.Converter;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BeanCopierUtils
 * <p>基于 CGLIB BeanCopier，提供高性能的对象属性拷贝。</p>
 * <p>主要功能：</p>
 * <ul>
 *   <li>单个对象深拷贝</li>
 *   <li>集合深拷贝</li>
 *   <li>支持自定义类型转换器 Converter</li>
 *   <li>内部缓存 BeanCopier 实例，避免重复生成</li>
 * </ul>
 *
 * @author vinjcent
 * @since 2023/3/28
 */
public final class BeanCopierUtils {

    /**
     * BeanCopier 缓存键：源类型 -> 目标类型
     */
    private static final Map<Key, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();

    private BeanCopierUtils() {
        // 工具类静态方法，无需实例化
    }

    /**
     * 单个对象拷贝
     *
     * @param source    源对象
     * @param targetCls 目标类型 class
     * @param <T>       目标类型
     * @return 拷贝后的新实例，source 为 null 则返回 null
     */
    public static <T> T copy(Object source, Class<T> targetCls) {
        return copy(source, targetCls, null);
    }

    /**
     * 单个对象拷贝，支持自定义类型转换器
     *
     * @param source    源对象
     * @param targetCls 目标类型 class
     * @param converter CGLIB Converter，用于属性类型不一致时的转换，可为 null
     * @param <T>       目标类型
     * @return 拷贝后的新实例，source 为 null 则返回 null
     */
    public static <T> T copy(Object source, Class<T> targetCls, Converter converter) {
        if (source == null) {
            return null;
        }
        Objects.requireNonNull(targetCls, "Target class must not be null");
        try {
            Constructor<T> ctor = targetCls.getDeclaredConstructor();
            ctor.setAccessible(true);
            T target = ctor.newInstance();
            doCopy(source, target, converter);
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate target class: " + targetCls, e);
        }
    }

    /**
     * List 拷贝
     *
     * @param sources   源集合
     * @param targetCls 目标类型 class
     * @param <S>       源类型
     * @param <T>       目标类型
     * @return 目标对象集合，sources 为 null 返回空集合
     */
    public static <S, T> List<T> copyList(List<S> sources, Class<T> targetCls) {
        if (sources == null) {
            return Collections.emptyList();
        }
        return sources.stream()
                .map(src -> copy(src, targetCls))
                .collect(Collectors.toList());
    }

    /**
     * List 拷贝，支持自定义 Converter
     *
     * @param sources   源集合
     * @param targetCls 目标类型 class
     * @param converter CGLIB Converter
     * @param <S>       源类型
     * @param <T>       目标类型
     * @return 目标对象集合
     */
    public static <S, T> List<T> copyList(List<S> sources, Class<T> targetCls, Converter converter) {
        if (sources == null) {
            return Collections.emptyList();
        }
        return sources.stream()
                .map(src -> copy(src, targetCls, converter))
                .collect(Collectors.toList());
    }

    /**
     * 实际拷贝方法，复用/创建 BeanCopier 实例并执行 copy
     */
    private static void doCopy(Object source, Object target, Converter converter) {
        Key key = new Key(source.getClass(), target.getClass(), converter != null);
        BeanCopier beanCopier = BEAN_COPIER_CACHE.computeIfAbsent(key,
                k -> BeanCopier.create(k.sourceClass, k.targetClass, k.withConverter));
        beanCopier.copy(source, target, converter);
    }

    /**
     * 缓存键：包含源类型、目标类型、是否使用 Converter
     */
    private static class Key {
        final Class<?> sourceClass;
        final Class<?> targetClass;
        final boolean withConverter;
        private final int hash;

        Key(Class<?> sourceClass, Class<?> targetClass, boolean withConverter) {
            this.sourceClass = sourceClass;
            this.targetClass = targetClass;
            this.withConverter = withConverter;
            this.hash = Objects.hash(sourceClass, targetClass, withConverter);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key that = (Key) o;
            return withConverter == that.withConverter
                    && sourceClass.equals(that.sourceClass)
                    && targetClass.equals(that.targetClass);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
