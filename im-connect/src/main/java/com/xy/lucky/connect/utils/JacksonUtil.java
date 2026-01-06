package com.xy.lucky.connect.utils;

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
import com.xy.lucky.connect.config.LogConstant;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;

/**
 * JSON 工具类
 * 封装 Jackson 常用操作，提供对象与 JSON 的互相转换
 * 支持泛型、JsonNode 操作和文件读写
 *
 * @author Lucky
 */
@Slf4j(topic = LogConstant.System)
public final class JacksonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 配置序列化选项
        MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.setDateFormat(new SimpleDateFormat(DATE_FORMAT));

        // 配置反序列化选项
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 启用多态类型处理（用于包含 @class 属性的 JSON）
        MAPPER.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING
        );

        log.debug("JacksonUtil 初始化完成，日期格式: {}", DATE_FORMAT);
    }
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private JacksonUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== JSON 字符串转对象 ====================

    /**
     * JSON 字符串转对象
     *
     * @param jsonString JSON 字符串
     * @param clazz      目标类型
     * @return 转换后的对象，失败返回 null
     */
    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 解析失败，目标类型：{}，JSON 字符串：{}", clazz.getName(), jsonString, e);
            return null;
        }
    }

    public static <T> T parseObject(File file, Class<T> clazz) {
        try {
            return MAPPER.readValue(file, clazz);
        } catch (IOException e) {
            log.error("从文件读取 JSON 失败，目标类型：{}，文件路径：{}", clazz.getName(), file.getPath(), e);
            return null;
        }
    }

    public static <T> T parseJSONArray(String jsonArray, TypeReference<T> reference) {
        try {
            return MAPPER.readValue(jsonArray, reference);
        } catch (JsonProcessingException e) {
            log.error("JSON 数组解析失败，目标类型：{}，JSON 字符串：{}", reference.getType(), jsonArray, e);
            return null;
        }
    }

    public static <T> T parseObject(Object obj, Class<T> clazz) {
        try {
            return MAPPER.convertValue(obj, clazz);
        } catch (Exception e) {
            log.error("对象转换失败，目标类型：{}，对象：{}", clazz.getName(), obj, e);
            return null;
        }
    }

    public static <T> T convertToActualObject(Object obj, Class<T> clazz) {
        try {
            if (obj instanceof LinkedHashMap) {
                String json = MAPPER.writeValueAsString(obj);
                return MAPPER.readValue(json, clazz);
            }
            return MAPPER.convertValue(obj, clazz);
        } catch (Exception e) {
            log.error("对象转换失败：{}", e.getMessage(), e);
            return null;
        }
    }

    public static <T> T parseObject(Object obj, TypeReference<T> typeReference) {
        try {
            JsonNode jsonNode = MAPPER.valueToTree(obj);
            return MAPPER.convertValue(jsonNode, typeReference);
        } catch (Exception e) {
            log.error("对象转换失败，目标类型：{}，对象：{}", typeReference.getType(), obj, e);
            return null;
        }
    }

    // ==================== 对象转 JSON ====================

    /**
     * 对象转 JSON 字符串
     */
    public static String toJSONString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return object instanceof String ? (String) object : MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转换为 JSON 字符串失败，对象：{}", object, e);
            return null;
        }
    }

    /**
     * 对象转字节数组
     */
    public static byte[] toByteArray(Object object) {
        try {
            return MAPPER.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("对象转换为字节数组失败，对象：{}", object, e);
            return null;
        }
    }

    /**
     * 对象写入文件
     */
    public static void objectToFile(File file, Object object) {
        try {
            MAPPER.writeValue(file, object);
        } catch (IOException e) {
            log.error("对象写入文件失败，文件路径：{}，对象：{}", file.getPath(), object, e);
        }
    }

    // ==================== JsonNode 操作 ====================

    /**
     * JSON 字符串转 JsonNode
     */
    public static JsonNode parseJSONObject(String jsonString) {
        try {
            return MAPPER.readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.error("JSON 字符串解析为 JsonNode 失败，JSON 字符串：{}", jsonString, e);
            return null;
        }
    }

    /**
     * 对象转 JsonNode
     */
    public static JsonNode parseJSONObject(Object object) {
        return MAPPER.valueToTree(object);
    }

    /**
     * JsonNode 转 JSON 字符串
     */
    public static String toJSONString(JsonNode jsonNode) {
        try {
            return MAPPER.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            log.error("JsonNode 转为 JSON 字符串失败，JsonNode：{}", jsonNode, e);
            return null;
        }
    }

    /**
     * 创建空的 ObjectNode
     */
    public static ObjectNode newJSONObject() {
        return MAPPER.createObjectNode();
    }

    /**
     * 创建空的 ArrayNode
     */
    public static ArrayNode newJSONArray() {
        return MAPPER.createArrayNode();
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
