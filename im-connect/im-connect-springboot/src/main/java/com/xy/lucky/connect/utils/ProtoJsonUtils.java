package com.xy.lucky.connect.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Values;

import java.io.IOException;
import java.util.*;

/**
 * ProtoJsonUtils
 * <p>
 * 功能说明（目标）
 * - 统一处理 protobuf Any <-> Java 值（POJO/Map/List/primitive）的转换
 * - 支持高效的 Map/List -> google.protobuf.Struct/Value 转换（避免不必要的 Json 字符串解析）
 * - 支持将 protobuf Message pack 到 Any（Any.pack）
 * - 支持稳健的 Any 解包（优先 protobuf 二进制解析，失败回退到 JSON 文本解析，再回退为原始 bytes）
 * <p>
 * 设计原则
 * - 兼容性优先：能兼容后端把 JSON 文本直接放进 Any.value（例如 value = "\"registrar\""）
 * - 性能优先：如果输入已经是 Map/List/基本类型，直接做内存级别转换，不通过中间 JSON 字符串解析
 * - 可复用性强：把解包/打包逻辑集中，encoder/decoder 只调用两个方法
 * <p>
 * 使用示例
 * - Any a = ProtoJsonUtils.packAny(someObj);
 * - Object obj = ProtoJsonUtils.unpackAny(a);
 */
public final class ProtoJsonUtils {

    // Jackson ObjectMapper（线程安全，可重用）
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // JsonFormat 用于 protobuf Message <-> json 字符串转换（稳健的 fallback）
    private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer().omittingInsignificantWhitespace();
    private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser().ignoringUnknownFields();

    private ProtoJsonUtils() {
        // 工具类，不允许实例化
    }

    // ===============================
    // 对外 API
    // ===============================

    /**
     * 把任意 Java 对象打包为 protobuf Any
     * <p>
     * 规则（优先级）：
     * 1. 如果 data 是 com.google.protobuf.Message 的实例，直接 Any.pack(message)
     * 2. 如果 data 是 Map / List / 基础类型（String/Number/Boolean/null），直接把它转换为 Struct/Value/ListValue（无中间 JSON 字符串）
     * 3. 如果 data 是其他 POJO（自定义对象），先用 Jackson 转为 Map，再走 2 的路径
     * <p>
     * 返回的 Any.typeUrl 为 type.googleapis.com/google.protobuf.Struct（若非 Message）
     *
     * @param data 任意 Java 对象
     * @return Any
     */
    public static Any packAny(Object data) {
        if (data == null) {
            // 空值 -> pack 一个空 Struct（也可以返回 empty Any，根据业务需要调整）
            Struct empty = Struct.newBuilder().build();
            return Any.pack(empty);
        }

        // 1) 如果已是 protobuf Message，直接 pack
        if (data instanceof Message) {
            return Any.pack((Message) data);
        }

        // 2) 如果是基础类型或者容器，直接构建 Struct/Value/ListValue
        if (isPrimitiveOrString(data) || data instanceof Map || data instanceof List) {
            Value v = javaToValue(data);
            // 若是 List 或 primitive，统一包装为 Struct 的 fields: { "value": v }，保持兼容
            Struct.Builder sb = Struct.newBuilder();
            sb.putFields("value", v);
            return Any.pack(sb.build());
        }

        // 3) 其他 POJO：先转 Map（避免直接 JsonFormat.merge 字符串解析的开销），再转换为 Struct
        try {
            // 将 POJO 转成 Map（保留嵌套结构）
            @SuppressWarnings("unchecked")
            Map<String, Object> map = MAPPER.convertValue(data, new TypeReference<Map<String, Object>>() {
            });
            Struct s = mapToStruct(map);
            return Any.pack(s);
        } catch (IllegalArgumentException ex) {
            // 万一 Jackson 无法转换（通常不会），兜底：把 data 转成 JSON 字符串然后用 JsonFormat 解析为 Struct
            try {
                String json = MAPPER.writeValueAsString(data);
                Struct.Builder sb = Struct.newBuilder();
                JSON_PARSER.merge(json, sb);
                return Any.pack(sb.build());
            } catch (Exception e) {
                // 最后兜底：pack 一个空 Struct（调用方可检测异常）
                // 这里不直接抛异常以保证调用链不会因单个字段失败而中断
                // 日志交给上层或使用记录机制
                return Any.pack(Struct.newBuilder().build());
            }
        }
    }

    /**
     * 将 com.google.protobuf.Any 解包为 Java 对象（Map/List/primitive 或 byte[]）
     * <p>
     * 解析策略（按优先级）：
     * 1. 若 typeUrl 指向 google.protobuf.Struct/Value/ListValue，优先使用对应 parseFrom（protobuf 二进制解析）
     * 2. 若解析失败或 typeUrl 不是上述之一，尝试把 Any.value 当作 UTF-8 JSON 文本解析（能兼容 value = "\"registrar\""）
     * 3. 再退化：使用 JsonFormat.printer(any) 打印为 JSON 字符串并用 Jackson 转成 Java 对象
     * 4. 最坏情况：返回原始 bytes（byte[]）
     * <p>
     * 返回值说明：
     * - 对于 Struct 通常返回 Map<String,Object>（如果 Struct 只有单个 "value" 字段，会直接返回该 value）
     * - 对于 ListValue 返回 List<Object>
     * - 对于 Value 返回对应的 primitive / Map / List
     * - 无法解析时返回 byte[]（原始二进制）
     *
     * @param any protobuf Any
     * @return Java 对象或 byte[]
     */
    public static Object unpackAny(Any any) {
        if (any == null) return null;

        String typeUrl = any.getTypeUrl();
        byte[] raw = any.getValue() == null ? new byte[0] : any.getValue().toByteArray();

        // 优先使用 protobuf 解析 well-known 类型（Struct/Value/ListValue）
        try {
            if (typeUrl != null && (typeUrl.endsWith("google.protobuf.Struct") || typeUrl.contains("Struct"))) {
                try {
                    Struct s = Struct.parseFrom(raw);
                    return structToMap(s);
                } catch (InvalidProtocolBufferException e) {
                    // 继续后续回退逻辑
                }
            }

            if (typeUrl != null && (typeUrl.endsWith("google.protobuf.ListValue") || typeUrl.contains("ListValue"))) {
                try {
                    ListValue lv = ListValue.parseFrom(raw);
                    return listToList(lv);
                } catch (InvalidProtocolBufferException e) {
                    // 继续后续回退逻辑
                }
            }

            if (typeUrl != null && (typeUrl.endsWith("google.protobuf.Value") || typeUrl.contains("Value"))) {
                try {
                    Value v = Value.parseFrom(raw);
                    return valueToJava(v);
                } catch (InvalidProtocolBufferException e) {
                    // 继续后续回退逻辑
                }
            }
        } catch (Throwable t) {
            // 保护性 catch，避免解析异常影响主流程
        }

        // 回退 1：尝试把 raw 当作 UTF-8 JSON 文本解析（处理服务端将 JSON bytes 放进 Any.value 的情况）
        try {
            String text = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
            if (looksLikeJsonText(text)) {
                try {
                    return MAPPER.readValue(text, Object.class);
                } catch (IOException ignored) {
                    // 解析失败 -> 继续下一个回退策略
                }
            }
        } catch (Exception ignored) {
            // 忽略
        }

        // 回退 2：使用 JsonFormat 打印 Any 的 JSON 表示，再用 Jackson 解析
        try {
            String printed = JSON_PRINTER.print(any);
            try {
                Object parsed = MAPPER.readValue(printed, Object.class);
                // 如果打印结果是 {"@type":"...","value": ...}，优先返回 value
                if (parsed instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) parsed;
                    if (m.size() == 1 && m.containsKey("value")) {
                        return m.get("value");
                    }
                }
                return parsed;
            } catch (IOException e) {
                // 解析失败 -> 继续
            }
        } catch (Exception ignored) {
            // JsonFormat 打印失败 -> 继续
        }

        // 最后退化：返回原始二进制
        return raw;
    }

    // ===============================
    // 辅助函数：Java <-> protobuf Struct/Value/ListValue
    // ===============================

    /**
     * 将 Java 值转换为 protobuf Value
     * 支持：null, Boolean, Number, String, Map -> Struct, List -> ListValue
     */
    @SuppressWarnings("unchecked")
    public static Value javaToValue(Object o) {
        if (o == null) return Values.ofNull();

        if (o instanceof Value) return (Value) o; // already a Value

        if (o instanceof Boolean) return Values.of((Boolean) o);
        if (o instanceof Number) {
            // 统一转为 double（protobuf Value.number_value 是 double）
            return Values.of(((Number) o).doubleValue());
        }
        if (o instanceof String) return Values.of((String) o);

        if (o instanceof Map) {
            Struct s = mapToStruct((Map<String, Object>) o);
            return Value.newBuilder().setStructValue(s).build();
        }

        if (o instanceof List) {
            ListValue.Builder lvb = ListValue.newBuilder();
            for (Object e : (List<Object>) o) {
                lvb.addValues(javaToValue(e));
            }
            return Value.newBuilder().setListValue(lvb.build()).build();
        }

        // 对于其他 POJO：先转 Map，然后递归转换
        try {
            Map<String, Object> map = MAPPER.convertValue(o, new TypeReference<Map<String, Object>>() {
            });
            Struct s = mapToStruct(map);
            return Value.newBuilder().setStructValue(s).build();
        } catch (IllegalArgumentException ex) {
            // 兜底：把 toString 作为字符串
            return Values.of(String.valueOf(o));
        }
    }

    /**
     * 将 Java Map 转换为 protobuf Struct（递归）
     * - Map 的 key 必须是 String，否则会调用 toString()
     */
    @SuppressWarnings("unchecked")
    public static Struct mapToStruct(Map<String, Object> map) {
        Struct.Builder sb = Struct.newBuilder();
        if (map == null) return sb.build();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey() == null ? "null" : e.getKey();
            Object v = e.getValue();
            sb.putFields(key, javaToValue(v));
        }
        return sb.build();
    }

    /**
     * 将 protobuf Struct 转回 Java Map（递归）
     */
    public static Map<String, Object> structToMap(Struct s) {
        if (s == null) return Collections.emptyMap();
        Map<String, Value> fields = s.getFieldsMap();
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Value> e : fields.entrySet()) {
            out.put(e.getKey(), valueToJava(e.getValue()));
        }
        // 如果只有单个 "value" 字段，常见约定是 unwrap：直接返回 value（保持与很多后端的约定一致）
        if (out.size() == 1 && out.containsKey("value")) {
            return Collections.singletonMap("value", out.get("value")); // 返回保留 "value" 字段（上层可自行 unwrap）
        }
        return out;
    }

    /**
     * 将 protobuf ListValue 转回 Java List
     */
    public static List<Object> listToList(ListValue lv) {
        if (lv == null) return Collections.emptyList();
        List<Object> out = new ArrayList<>(lv.getValuesList().size());
        for (Value v : lv.getValuesList()) out.add(valueToJava(v));
        return out;
    }

    /**
     * 将 protobuf Value 转回 Java 对象
     */
    public static Object valueToJava(Value v) {
        if (v == null) return null;
        switch (v.getKindCase()) {
            case NULL_VALUE:
                return null;
            case BOOL_VALUE:
                return v.getBoolValue();
            case NUMBER_VALUE:
                return v.getNumberValue();
            case STRING_VALUE:
                return v.getStringValue();
            case STRUCT_VALUE:
                return structToMap(v.getStructValue());
            case LIST_VALUE:
                return listToList(v.getListValue());
            case KIND_NOT_SET:
            default:
                return null;
        }
    }

    // ===============================
    // 少量工具函数
    // ===============================

    /**
     * 判断一个对象是否为基本类型或字符串
     */
    private static boolean isPrimitiveOrString(Object o) {
        return o == null ||
                o instanceof String ||
                o instanceof Boolean ||
                o instanceof Number;
    }

    /**
     * 简单启发式判断给定文本是否看起来像 JSON（不保证准确，但能过滤大多数二进制）
     */
    private static boolean looksLikeJsonText(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        char c = t.charAt(0);
        return c == '{' || c == '[' || c == '"' || c == '-' || (c >= '0' && c <= '9') || t.startsWith("true")
                || t.startsWith("false") || t.startsWith("null");
    }
}
