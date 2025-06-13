package com.xy.connect.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;


/**
 * JacksonUtil - 基于 Jackson 的 JSON 工具类，提供简洁易用的序列化/反序列化方法
 * 主要特性：
 * 1. 对象 ↔ JSON 字符串
 * 2. 对象 ↔ JSON 字节数组
 * 3. 文件读写
 * 4. 泛型、LinkedHashMap、JsonNode 处理
 * 5. 忽略未知属性、日期格式统一
 */
@Slf4j
public class JacksonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    static {
        // 全量字段序列化
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 取消时间戳格式
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 忽略空 Bean 错误
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 设置统一日期格式
        mapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
        // 忽略未知属性，防止反序列化失败
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JacksonUtil() {
        // 私有构造，禁止实例化
    }

    /**
     * 将 JSON 字符串反序列化为指定类型对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) return null;
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            log.error("反序列化 JSON 到 {} 失败，json={}", clazz.getName(), json, e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为泛型类型
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || typeRef == null) return null;
        try {
            return mapper.readValue(json, typeRef);
        } catch (IOException e) {
            log.error("反序列化 JSON 到 {} 失败，json={}", typeRef.getType(), json, e);
            return null;
        }
    }

    /**
     * 将对象转换为指定类型（支持 LinkedHashMap 转换）
     */
    public static <T> T convert(Object obj, Class<T> clazz) {
        if (obj == null || clazz == null) return null;
        try {
            return mapper.convertValue(obj, clazz);
        } catch (IllegalArgumentException e) {
            log.error("对象转换为 {} 失败，对象={}", clazz.getName(), obj, e);
            return null;
        }
    }

    /**
     * 将对象转换为指定泛型类型
     */
    public static <T> T convert(Object obj, TypeReference<T> typeRef) {
        if (obj == null || typeRef == null) return null;
        try {
            // 先序列化为 JSON，再反序列化以确保类型安全
            String json = mapper.writeValueAsString(obj);
            return mapper.readValue(json, typeRef);
        } catch (IOException e) {
            log.error("对象转换为 {} 失败，对象={}", typeRef.getType(), obj, e);
            return null;
        }
    }


    // 方法：将 LinkedHashMap 转换为目标对象
    public static <T> T convertToActualObject(Object obj, Class<T> clazz) {
        try {
            if (obj instanceof LinkedHashMap) {
                // 如果 obj 是 LinkedHashMap，尝试转换为目标对象
                String json = mapper.writeValueAsString(obj);
                return mapper.readValue(json, clazz);
            }
            return mapper.convertValue(obj, clazz);
        } catch (Exception e) {
            System.err.println("对象转换失败：" + e.getMessage());
            return null;
        }
    }


    /**
     * 将对象转换为 JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转换为 JSON 失败，对象={}", obj, e);
            return null;
        }
    }

    /**
     * 将对象转换为 JSON 字节数组
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) return null;
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转换为字节数组失败，对象={}", obj, e);
            return new byte[0];
        }
    }

    /**
     * 将对象写入到文件（覆盖）
     */
    public static void toFile(File file, Object obj) {
        if (file == null || obj == null) return;
        try {
            mapper.writeValue(file, obj);
        } catch (IOException e) {
            log.error("写入 JSON 到文件失败，file={}, object={}", file.getPath(), obj, e);
        }
    }

    /**
     * 从文件读取并反序列化为指定类型
     */
    public static <T> T fromFile(File file, Class<T> clazz) {
        if (file == null || clazz == null) return null;
        try {
            return mapper.readValue(file, clazz);
        } catch (IOException e) {
            log.error("从文件读取 JSON 失败，file={}, class={}", file.getPath(), clazz.getName(), e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串解析为 JsonNode
     */
    public static JsonNode toNode(String json) {
        if (json == null) return null;
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            log.error("解析 JSON 为 JsonNode 失败，json={}", json, e);
            return null;
        }
    }

    /**
     * 将对象转换为 JsonNode
     */
    public static JsonNode toNode(Object obj) {
        if (obj == null) return null;
        return mapper.valueToTree(obj);
    }

    /**
     * 创建空 ObjectNode
     */
    public static ObjectNode newObject() {
        return mapper.createObjectNode();
    }

    /**
     * 创建空 ArrayNode
     */
    public static ArrayNode newArray() {
        return mapper.createArrayNode();
    }

    /**
     * 从 JsonNode 中获取字段文本值
     */
    public static String getString(JsonNode node, String key) {
        return (node != null && node.has(key)) ? node.get(key).asText() : null;
    }

    /**
     * 从 JsonNode 中获取字段整数值
     */
    public static Integer getInt(JsonNode node, String key) {
        return (node != null && node.has(key)) ? node.get(key).asInt() : null;
    }

    /**
     * 从 JsonNode 中获取字段布尔值
     */
    public static Boolean getBoolean(JsonNode node, String key) {
        return (node != null && node.has(key)) ? node.get(key).asBoolean() : null;
    }

    /**
     * 从 JsonNode 中获取子 JsonNode
     */
    public static JsonNode getNode(JsonNode node, String key) {
        return (node != null && node.has(key)) ? node.get(key) : null;
    }
}
