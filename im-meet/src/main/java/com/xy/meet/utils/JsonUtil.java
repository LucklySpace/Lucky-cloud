package com.xy.meet.utils;

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
 * JSON 工具类，封装了 Jackson 的常用方法，提供类似 Fastjson 的使用体验
 */
@Slf4j
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 时间日期格式
    private static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    static {
        // 对象的所有字段全部列入序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 取消默认转换 timestamps 形式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 忽略空 Bean 转 json 的错误
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 设置日期格式
        objectMapper.setDateFormat(new SimpleDateFormat(STANDARD_FORMAT));
        // 忽略在 JSON 字符串中存在，但在 Java 对象中不存在对应属性的情况，防止错误
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * ===========================以下是从JSON中获取对象====================================
     */
    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败，目标类型：{}，JSON 字符串：{}", clazz.getName(), jsonString, e);
            return null;
        }
    }

    public static <T> T parseObject(File file, Class<T> clazz) {
        try {
            return objectMapper.readValue(file, clazz);
        } catch (IOException e) {
            log.error("从文件读取 JSON 失败，目标类型：{}，文件路径：{}", clazz.getName(), file.getPath(), e);
            return null;
        }
    }

    // 支持泛型对象的反序列化
    public static <T> T parseJSONArray(String jsonArray, TypeReference<T> reference) {
        try {
            return objectMapper.readValue(jsonArray, reference);
        } catch (JsonProcessingException e) {
            log.error("JSON 数组解析失败，目标类型：{}，JSON 字符串：{}", reference.getType(), jsonArray, e);
            return null;
        }
    }


    public static <T> T parseObject(Object obj, Class<T> clazz) {
        try {
            return objectMapper.convertValue(obj, clazz);
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
                String json = objectMapper.writeValueAsString(obj);
                return objectMapper.readValue(json, clazz);
            }
            return objectMapper.convertValue(obj, clazz);
        } catch (Exception e) {
            System.err.println("对象转换失败：" + e.getMessage());
            return null;
        }
    }

    public static <T> T parseObject(Object obj, TypeReference<T> typeReference) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.convertValue(json, typeReference);
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
            return object instanceof String ? (String) object : objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转换为 JSON 字符串失败，对象：{}", object, e);
            return null;
        }
    }

    public static byte[] toByteArray(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("对象转换为字节数组失败，对象：{}", object, e);
            return null;
        }
    }

    public static void objectToFile(File file, Object object) {
        try {
            objectMapper.writeValue(file, object);
        } catch (IOException e) {
            log.error("对象写入文件失败，文件路径：{}，对象：{}", file.getPath(), object, e);
        }
    }

    /**
     * ===========================与JsonNode相关的操作====================================
     */
    public static JsonNode parseJSONObject(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.error("JSON 字符串解析为 JsonNode 失败，JSON 字符串：{}", jsonString, e);
            return null;
        }
    }

    public static JsonNode parseJSONObject(Object object) {
        return objectMapper.valueToTree(object);
    }

    public static String toJSONString(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            log.error("JsonNode 转为 JSON 字符串失败，JsonNode：{}", jsonNode, e);
            return null;
        }
    }

    public static ObjectNode newJSONObject() {
        return objectMapper.createObjectNode();
    }

    public static ArrayNode newJSONArray() {
        return objectMapper.createArrayNode();
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
