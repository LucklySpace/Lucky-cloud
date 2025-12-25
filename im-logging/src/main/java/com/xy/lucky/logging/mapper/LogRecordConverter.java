package com.xy.lucky.logging.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.domain.LogLevel;
import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
                .traceId(po.getTraceId())
                .spanId(po.getSpanId())
                .thread(po.getThread())
                .message(po.getMessage())
                .exception(po.getException())
                .tags(parseTagsQuietly(po.getTags()))
                .context(parseContextQuietly(po.getContext()))
                .build();
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

