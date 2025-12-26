package com.xy.lucky.logging.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.domain.LogLevel;
import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LogRecordConverter {

    private final ObjectMapper objectMapper;

    public LogPo toPo(LogRecordVo record) {
        if (record == null) {
            return null;
        }
        return LogPo.builder()
                .id(record.getId())
                .ts(record.getTimestamp())
                .level(record.getLevel() != null ? record.getLevel().name() : null)
                .module(record.getModule())
                .service(record.getService())
                .address(record.getAddress())
                .env(record.getEnv())
                .traceId(record.getTraceId())
                .spanId(record.getSpanId())
                .thread(record.getThread())
                .message(record.getMessage())
                .exception(record.getException())
                .tags(writeJsonQuietly(record.getTags()))
                .context(writeJsonQuietly(record.getContext()))
                .build();
    }

    public LogRecordVo toVo(LogPo po) {
        if (po == null) {
            return null;
        }
        return LogRecordVo.builder()
                .id(po.getId())
                .timestamp(po.getTs())
                .level(parseLevelQuietly(po.getLevel()))
                .module(po.getModule())
                .service(po.getService())
                .address(po.getAddress())
                .env(po.getEnv())
                .traceId(po.getTraceId())
                .spanId(po.getSpanId())
                .thread(po.getThread())
                .message(po.getMessage())
                .exception(po.getException())
                .tags(parseTagsQuietly(po.getTags()))
                .context(parseContextQuietly(po.getContext()))
                .build();
    }

    public LogRecordVo fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        String id = asString(map, "id");
        Instant ts = parseInstant(asString(map, "timestamp"));
        LogLevel level = parseLevelQuietly(firstNonNull(map, "level", "lvl", "severity"));
        String module = asString(map, "module");
        String service = asString(map, "service");
        String address = firstNonNull(map, "address", "host");
        String env = firstNonNull(map, "env", "environment");
        String traceId = firstNonNull(map, "traceId", "trace_id");
        String spanId = firstNonNull(map, "spanId", "span_id");
        String thread = asString(map, "thread");
        String message = firstNonNull(map, "message", "msg");
        Object exceptionObj = map.get("exception");
        String exception = exceptionObj instanceof String ? (String) exceptionObj : writeJsonQuietly(exceptionObj);
        Set<String> tags = extractTags(map.get("tags"));
        Map<String, Object> ctx = extractContext(map);
        if (module == null && service != null) {
            module = service;
        }
        return LogRecordVo.builder()
                .id(id)
                .timestamp(ts)
                .level(level)
                .module(module)
                .service(service)
                .address(address)
                .env(env)
                .traceId(traceId)
                .spanId(spanId)
                .thread(thread)
                .message(message)
                .exception(exception)
                .tags(tags)
                .context(ctx)
                .build();
    }

    private String asString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private String firstNonNull(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }

    private Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Instant.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<String> extractTags(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Set<?> set) {
            Set<String> out = new LinkedHashSet<>();
            for (Object o : set) if (o != null) out.add(String.valueOf(o));
            return out.isEmpty() ? null : out;
        }
        String s = obj.toString().trim();
        if (s.startsWith("[")) {
            try {
                return objectMapper.readValue(s, new TypeReference<LinkedHashSet<String>>() {
                });
            } catch (Exception ignored) {
            }
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : s.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out.isEmpty() ? null : out;
    }

    private Map<String, Object> extractContext(Map<String, Object> map) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String k = e.getKey();
            if (isCoreField(k)) continue;
            ctx.put(k, e.getValue());
        }
        return ctx.isEmpty() ? null : ctx;
    }

    private boolean isCoreField(String k) {
        return switch (k) {
            case "id", "timestamp", "level", "lvl", "severity", "module", "service", "address", "host",
                 "traceId", "trace_id", "spanId", "span_id", "thread", "message", "msg", "exception", "tags" -> true;
            default -> false;
        };
    }

    private LogLevel parseLevelQuietly(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        try {
            return LogLevel.valueOf(level);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String writeJsonQuietly(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<String> parseTagsQuietly(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<LinkedHashSet<String>>() {
                });
            } catch (Exception ignored) {
                return null;
            }
        }
        Set<String> tags = new LinkedHashSet<>();
        for (String part : trimmed.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                tags.add(t);
            }
        }
        return tags.isEmpty() ? null : tags;
    }

    private Map<String, Object> parseContextQuietly(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        try {
            return objectMapper.readValue(trimmed, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ignored) {
            return null;
        }
    }
}
