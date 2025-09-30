package com.xy.grpc.core.serialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 向后兼容且更容错的 JsonSerializer。
 * <p>
 * 特性：
 * - 使用线程安全的单例 ObjectMapper（重用以提升性能）。
 * - 如果 payload 是 JSON 数组，而目标是单一类型（非数组/集合/Map/byte[]），
 * 则当数组长度==1 时自动取第一个元素反序列化；长度>1 时对 String 返回 JSON 数组字符串，其它类型抛异常。
 * - 新增 deserializeToArray 方法，支持将 JSON 数组反序列化为指定类型数组（适用于多参数方法）。
 */
@Component("JsonSerializer")
public class JsonSerializer implements Serializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final int PREVIEW_MAX_LEN = 256;

    static {
        // 配置：忽略 null 值序列化
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 配置：日期序列化为字符串而非时间戳
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 配置：自定义日期格式（建议升级到 JavaTimeModule 以支持 LocalDate 等）
        MAPPER.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 配置：忽略未知属性，反序列化更容错
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 配置：允许单引号（宽松 JSON 解析）
        MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 配置：不允许无引号字段名（安全考虑，未开启）
        MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj == null) {
            return new byte[0];
        }
        return MAPPER.writeValueAsBytes(obj);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        int firstNonWsIndex = firstNonWhitespaceIndex(bytes);
        if (firstNonWsIndex < 0) {
            return null;
        }

        byte firstByte = bytes[firstNonWsIndex];

        // 特殊处理：如果目标是 byte[]，直接返回原始字节
        if (clazz == byte[].class) {
            return (T) bytes;
        }

        boolean isPayloadArray = (firstByte == '[');

        // 兼容逻辑：payload 是数组，但目标类型不是数组/集合/Map/byte[]
        if (isPayloadArray && !isArrayOrCollectionOrMap(clazz)) {
            JsonNode root = parseAsTree(bytes);
            if (!root.isArray()) {
                // 首字节误判，退回常规路径
                return readValue(bytes, clazz);
            }

            int size = root.size();
            if (size == 0) {
                return null; // 空数组视为 null
            } else if (size == 1) {
                JsonNode element = root.get(0);
                if (element.isNull()) {
                    return null; // [null] 视为 null
                }
                return treeToValue(element, clazz);
            } else { // size > 1
                if (clazz == String.class) {
                    // 对于 String，返回原始 JSON 字符串
                    return (T) new String(bytes, UTF_8);
                }
                throw new RuntimeException("Cannot deserialize JSON array of length " + size +
                        " into single value of type " + clazz.getName() +
                        ". Suggestion: Use array/collection in method signature or send single value. Payload preview: " + safePreview(bytes));
            }
        }

        // 常规反序列化
        return readValue(bytes, clazz);
    }

    /**
     * 新增方法：将 JSON 数组反序列化为指定类型数组。
     * 用于多参数场景（e.g., GRPC 方法有多个参数，payload 为 [arg1, arg2, ...]）。
     *
     * @param bytes      输入字节（必须是 JSON 数组）
     * @param paramTypes 每个元素的目标类型数组（长度必须匹配 JSON 数组大小）
     * @return 反序列化后的对象数组
     * @throws Exception 如果解析失败或大小不匹配
     */
    public Object[] deserializeToArray(byte[] bytes, Class<?>[] paramTypes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Payload bytes cannot be empty for array deserialization");
        }
        if (paramTypes == null || paramTypes.length == 0) {
            throw new IllegalArgumentException("Param types cannot be empty");
        }

        JsonNode root = parseAsTree(bytes);
        if (!root.isArray()) {
            throw new RuntimeException("Payload is not a JSON array. Preview: " + safePreview(bytes));
        }

        int size = root.size();
        if (size != paramTypes.length) {
            throw new RuntimeException("JSON array size " + size + " does not match expected param types length " + paramTypes.length +
                    ". Preview: " + safePreview(bytes));
        }

        Object[] args = new Object[size];
        for (int i = 0; i < size; i++) {
            JsonNode node = root.get(i);
            if (node.isNull() && !paramTypes[i].isPrimitive()) {
                args[i] = null;
            } else {
                args[i] = treeToValue(node, paramTypes[i]);
            }
        }
        return args;
    }

    private boolean isArrayOrCollectionOrMap(Class<?> clazz) {
        return clazz.isArray() ||
                java.util.Collection.class.isAssignableFrom(clazz) ||
                java.util.Map.class.isAssignableFrom(clazz) ||
                clazz == byte[].class;
    }

    private JsonNode parseAsTree(byte[] bytes) throws Exception {
        try {
            return MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw wrapException("JSON parse error when reading as tree. Payload preview: " + safePreview(bytes), e);
        }
    }

    private <T> T readValue(byte[] bytes, Class<T> clazz) throws Exception {
        try {
            return MAPPER.readValue(bytes, clazz);
        } catch (JsonProcessingException e) {
            throw wrapException("JSON deserialize error to " + clazz.getName() + ". Payload preview: " + safePreview(bytes), e);
        }
    }

    private <T> T treeToValue(JsonNode node, Class<T> clazz) throws Exception {
        try {
            return MAPPER.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw wrapException("Failed to convert JSON node to " + clazz.getName() + ". Node: " + node.toString(), e);
        }
    }

    private RuntimeException wrapException(String message, Exception cause) {
        return new RuntimeException(message, cause);
    }

    private int firstNonWhitespaceIndex(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (!isWhitespace(bytes[i])) {
                return i;
            }
        }
        return -1;
    }

    private boolean isWhitespace(byte b) {
        return b == ' ' || b == '\n' || b == '\r' || b == '\t';
    }

    private String safePreview(byte[] bytes) {
        try {
            int len = Math.min(bytes.length, PREVIEW_MAX_LEN);
            return new String(bytes, 0, len, UTF_8) + (bytes.length > PREVIEW_MAX_LEN ? "..." : "");
        } catch (Exception e) {
            return "[non-UTF8 or binary payload]";
        }
    }

    @Override
    public String contentType() {
        return "application/json";
    }
}