package com.xy.utils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;

/**
 * JSON 工具类，封装了 Jackson 的常用方法
 */
@Slf4j
public class JacksonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    // 时间日期格式
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
        // 启用默认类型推断，支持 @class 属性
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.EVERYTHING);
    }

    /**
     * ===========================以下是从JSON中获取对象====================================
     */
    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败，目标类型：{}，JSON 字符串：{}", clazz.getName(), jsonString, e);
            return null;
        }
    }

    public static <T> T parseObject(File file, Class<T> clazz) {
        try {
            return mapper.readValue(file, clazz);
        } catch (IOException e) {
            log.error("从文件读取 JSON 失败，目标类型：{}，文件路径：{}", clazz.getName(), file.getPath(), e);
            return null;
        }
    }

    // 支持泛型对象的反序列化
    public static <T> T parseJSONArray(String jsonArray, TypeReference<T> reference) {
        try {
            return mapper.readValue(jsonArray, reference);
        } catch (JsonProcessingException e) {
            log.error("JSON 数组解析失败，目标类型：{}，JSON 字符串：{}", reference.getType(), jsonArray, e);
            return null;
        }
    }


    public static <T> T parseObject(Object obj, Class<T> clazz) {
        try {
            return mapper.convertValue(obj, clazz);
        } catch (Exception e) {
            log.error("对象转换失败，目标类型：{}，对象：{}", clazz.getName(), obj, e);
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

    public static <T> T parseObject(Object obj, TypeReference<T> typeReference) {
        try {
            String json = mapper.writeValueAsString(obj);
            return mapper.convertValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("对象转换失败，目标类型：{}，对象：{} 错误", typeReference.getType(), obj, e);
            return null;
        }
    }

    /**
     * ===========================以下是将对象转为JSON====================================
     */
    public static String toJSONString(Object object) {
        try {
            return object instanceof String ? (String) object : mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转换为 JSON 字符串失败，对象：{}", object, e);
            return null;
        }
    }

    public static byte[] toByteArray(Object object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("对象转换为字节数组失败，对象：{}", object, e);
            return null;
        }
    }

    public static void objectToFile(File file, Object object) {
        try {
            mapper.writeValue(file, object);
        } catch (IOException e) {
            log.error("对象写入文件失败，文件路径：{}，对象：{}", file.getPath(), object, e);
        }
    }

    /**
     * ===========================与JsonNode相关的操作====================================
     */
    public static JsonNode parseJSONObject(String jsonString) {
        try {
            return mapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.error("JSON 字符串解析为 JsonNode 失败，JSON 字符串：{}", jsonString, e);
            return null;
        }
    }

    public static JsonNode parseJSONObject(Object object) {
        return mapper.valueToTree(object);
    }

    public static String toJSONString(JsonNode jsonNode) {
        try {
            return mapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            log.error("JsonNode 转为 JSON 字符串失败，JsonNode：{}", jsonNode, e);
            return null;
        }
    }

    public static ObjectNode newJSONObject() {
        return mapper.createObjectNode();
    }

    public static ArrayNode newJSONArray() {
        return mapper.createArrayNode();
    }

    /**
     * ===========获取JsonNode中字段的方法===========
     */
    public static String getString(JsonNode jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.get(key).asText() : null;
    }

    public static Integer getInteger(JsonNode jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.get(key).asInt() : null;
    }

    public static Boolean getBoolean(JsonNode jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.get(key).asBoolean() : null;
    }

    public static JsonNode getJSONObject(JsonNode jsonObject, String key) {
        return jsonObject.has(key) ? jsonObject.get(key) : null;
    }
}
