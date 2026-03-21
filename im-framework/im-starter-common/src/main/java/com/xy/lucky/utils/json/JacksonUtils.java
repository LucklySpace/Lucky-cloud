package com.xy.lucky.utils.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
public class JacksonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper compatTypedMapper = new ObjectMapper();
    private static final ObjectMapper legacyWrapperTypedMapper = new ObjectMapper();

    // 时间日期格式
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    static {
        configureMapper(mapper);
        configureMapper(compatTypedMapper);
        configureMapper(legacyWrapperTypedMapper);

        compatTypedMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        legacyWrapperTypedMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING
        );
    }

    /**
     * ===========================以下是从JSON中获取对象====================================
     */
    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (Exception e) {
            try {
                return compatTypedMapper.readValue(jsonString, clazz);
            } catch (Exception ignored) {
            }
            try {
                return legacyWrapperTypedMapper.readValue(jsonString, clazz);
            } catch (Exception ignored) {
            }
            log.error("JSON 解析失败，目标类型：{}，JSON 字符串：{}", clazz.getName(), jsonString, e);
            return null;
        }
    }

    public static <T> T parseObject(File file, Class<T> clazz) {
        try {
            return mapper.readValue(file, clazz);
        } catch (Exception e) {
            try {
                return compatTypedMapper.readValue(file, clazz);
            } catch (Exception ignored) {
            }
            try {
                return legacyWrapperTypedMapper.readValue(file, clazz);
            } catch (Exception ignored) {
            }
            log.error("从文件读取 JSON 失败，目标类型：{}，文件路径：{}", clazz.getName(), file.getPath(), e);
            return null;
        }
    }

    // 支持泛型对象的反序列化
    public static <T> T parseJSONArray(String jsonArray, TypeReference<T> reference) {
        try {
            return mapper.readValue(jsonArray, reference);
        } catch (Exception e) {
            try {
                return compatTypedMapper.readValue(jsonArray, reference);
            } catch (Exception ignored) {
            }
            try {
                return legacyWrapperTypedMapper.readValue(jsonArray, reference);
            } catch (Exception ignored) {
            }
            log.error("JSON 数组解析失败，目标类型：{}，JSON 字符串：{}", reference.getType(), jsonArray, e);
            return null;
        }
    }


    public static <T> T parseObject(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String jsonString) {
            return parseObject(jsonString, clazz);
        }
        try {
            return mapper.convertValue(obj, clazz);
        } catch (Exception e) {
            try {
                return compatTypedMapper.convertValue(obj, clazz);
            } catch (Exception ignored) {
            }
            try {
                return legacyWrapperTypedMapper.convertValue(obj, clazz);
            } catch (Exception ignored) {
            }
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
        if (obj == null) {
            return null;
        }
        if (obj instanceof String jsonString) {
            try {
                return mapper.readValue(jsonString, typeReference);
            } catch (Exception e1) {
                try {
                    return compatTypedMapper.readValue(jsonString, typeReference);
                } catch (Exception ignored) {
                }
                try {
                    return legacyWrapperTypedMapper.readValue(jsonString, typeReference);
                } catch (Exception ignored) {
                }
            }
        }
        try {
            JsonNode jsonNode = mapper.valueToTree(obj);
            return mapper.convertValue(jsonNode, typeReference);
        } catch (Exception e) {
            try {
                JsonNode jsonNode = compatTypedMapper.valueToTree(obj);
                return compatTypedMapper.convertValue(jsonNode, typeReference);
            } catch (Exception ignored) {
            }
            try {
                JsonNode jsonNode = legacyWrapperTypedMapper.valueToTree(obj);
                return legacyWrapperTypedMapper.convertValue(jsonNode, typeReference);
            } catch (Exception ignored) {
            }
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

    private static void configureMapper(ObjectMapper objectMapper) {
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
