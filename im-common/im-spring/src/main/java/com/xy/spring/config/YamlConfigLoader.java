package com.xy.spring.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML 配置加载器，用于将 application.yml 扁平化为 key-value 形式，并支持类型转换
 */
public class YamlConfigLoader {

    // 存储扁平化的配置项，如 "redis.host" -> "127.0.0.1"
    private static final Map<String, Object> properties = new HashMap<>();

    // 加载并解析 application.yml
//    static {
//        try (InputStream in = YamlConfigLoader.class.getClassLoader().getResourceAsStream("application.yml")) {
//            if (in == null) throw new RuntimeException("未找到 application.yml 文件");
//
//            Map<String, Object> yamlData = new Yaml().load(in);
//            flatten("", yamlData);
//        } catch (Exception e) {
//            throw new RuntimeException("YAML 读取失败", e);
//        }
//    }

    public static void load() {
        try (InputStream in = YamlConfigLoader.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (in == null) throw new RuntimeException("未找到 application.yml 文件");

            Map<String, Object> yamlData = new Yaml().load(in);
            flatten("", yamlData);
        } catch (Exception e) {
            throw new RuntimeException("YAML 读取失败", e);
        }
    }

    /**
     * 递归将嵌套的 YAML 配置扁平化，便于通过路径访问
     * 如： redis.host -> 127.0.0.1
     */
    private static void flatten(String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatten(key, (Map<String, Object>) value);
            } else {
                properties.put(key, value);
            }
        }
    }

    /**
     * 根据 key 获取配置项，并转换为目标字段类型
     *
     * @param key   配置路径，如 redis.port
     * @param field 目标字段（用于推断泛型）
     */
    public static Object get(String key, Field field) {
        Object rawValue = properties.get(key);
        String val = rawValue != null ? rawValue.toString().trim() : "";
        return convertValue(field.getType(), val, field);
    }

    /**
     * 手动添加或覆盖配置
     */
    public static void put(String key, String value) {
        properties.put(key, value);
    }

    /**
     * 将字符串值转换为指定类型（支持基础类型、List、Map、数组）
     */
    private static Object convertValue(Class<?> type, String val, Field field) {
        if (type == String.class) return val;
        if (type == int.class || type == Integer.class) return Integer.parseInt(val);
        if (type == long.class || type == Long.class) return Long.parseLong(val);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(val);
        if (type == String[].class) return val.split(",");

        if (type == List.class && field != null) {
            return convertToList(val, field);
        }

        if (type == Map.class) {
            return parseMap(val);
        }

        // 默认按字符串返回
        return val;
    }

    /**
     * 将字符串转为 List<T>，如 "[1,2,3]" -> List<Integer>
     */
    private static List<Object> convertToList(String val, Field field) {
        val = val.replaceAll("[\\[\\]\\s]", "");
        String[] items = val.split(",");

        List<Object> result = new ArrayList<>();
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            Type itemType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            for (String item : items) {
                result.add(convertSimpleValue(itemType, item));
            }
        }
        return result;
    }

    /**
     * 将字符串转换为基本类型值
     */
    private static Object convertSimpleValue(Type type, String val) {
        if (type == Integer.class || type == int.class) return Integer.parseInt(val);
        if (type == Long.class || type == long.class) return Long.parseLong(val);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(val);
        return val;
    }

    /**
     * 将字符串转为 Map<String, String>
     * 格式："{k1=v1, k2=v2}"
     */
    private static Map<String, String> parseMap(String val) {
        Map<String, String> map = new HashMap<>();
        val = val.trim();
        if (!val.startsWith("{") || !val.endsWith("}")) return map;

        String content = val.substring(1, val.length() - 1);
        for (String pair : content.split(",")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}