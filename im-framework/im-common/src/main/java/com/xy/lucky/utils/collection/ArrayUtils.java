package com.xy.lucky.utils.collection;


import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;



/**
 * Array 工具类
 */
public class ArrayUtils {

    /**
     * 将 object 和 newElements 合并成一个数组
     *
     * @param object 对象
     * @param newElements 数组
     * @param <T> 泛型
     * @return 结果数组
     */
    @SafeVarargs
    public static <T> Consumer<T>[] append(Consumer<T> object, Consumer<T>... newElements) {
        if (object == null) {
            return newElements;
        }
        Consumer<T>[] result = newArray(Consumer.class, 1 + newElements.length);
        result[0] = object;
        System.arraycopy(newElements, 0, result, 1, newElements.length);
        return result;
    }

    public static <T, V> V[] toArray(Collection<T> from, Function<T, V> mapper) {
        return toArray(CollectionUtils.convertList(from, mapper));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection<T> from) {
        if (CollectionUtils.isEmpty(from)) {
            return (T[]) (new Object[0]);
        }
        return toArray(from, (Class<T>) from.iterator().next().getClass());
    }

    public static <T> T[] toArray(Collection<T> from, Class<T> type) {
        return from.toArray(newArray(type, from.size()));
    }

    /**
     * 根据指定的元素类型和长度创建新数组
     *
     * @param type   数组元素类型
     * @param length 数组长度
     * @param <T>    数组元素类型
     * @return 新数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<T> type, int length) {
        return (T[]) Array.newInstance(type, length);
    }

    public static <T> T get(T[] array, int index) {
        if (null == array || index >= array.length) {
            return null;
        }
        return array[index];
    }

    /**
     * 判断字符串数组中是否包含指定的字符串
     *
     * @param strs 字符串数组
     * @param str  字符串
     * @return 是否包含
     */
    public static boolean contains(String[] strs, String str) {
        return Arrays.stream(strs).findAny().filter(s -> s.equals(str)).isPresent();
    }
}