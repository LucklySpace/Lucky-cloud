package com.xy.lucky.utils.collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Map 工具类
 *
 * @author 芋道源码
 */
public class MapUtils {

    /**
     * 从Map中获取指定key的值，如果值存在则使用consumer处理
     *
     * @param map      哈希表
     * @param key      key
     * @param consumer 进一步处理的逻辑
     * @param <K>      键类型
     * @param <V>      值类型
     */
    public static <K, V> void findAndThen(Map<K, V> map, K key, Consumer<V> consumer) {
        if (key == null || map == null || map.isEmpty()) {
            return;
        }
        V value = map.get(key);
        if (value == null) {
            return;
        }
        consumer.accept(value);
    }

    /**
     * 检查Map是否为空
     *
     * @param map 待检查的Map
     * @return 如果为空返回true，否则返回false
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 检查Map是否不为空
     *
     * @param map 待检查的Map
     * @return 如果不为空返回true，否则返回false
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 根据键值对列表创建Map
     *
     * @param keyValues 键值对列表
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 创建的Map
     */
    public static <K, V> Map<K, V> createMap(List<KeyValue<K, V>> keyValues) {
        if (keyValues == null || keyValues.isEmpty()) {
            return new HashMap<>();
        }
        Map<K, V> map = new HashMap<>(keyValues.size());
        keyValues.forEach(keyValue -> map.put(keyValue.getKey(), keyValue.getValue()));
        return map;
    }

    /**
     * 键值对封装类
     *
     * @param <K> 键类型
     * @param <V> 值类型
     */
    public static class KeyValue<K, V> {
        private final K key;
        private final V value;

        public KeyValue(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}