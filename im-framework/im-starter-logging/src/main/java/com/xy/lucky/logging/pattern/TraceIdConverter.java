package com.xy.lucky.logging.pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.DynamicConverter;

public class TraceIdConverter extends DynamicConverter<ILoggingEvent> {
    @Override
    public String convert(ILoggingEvent event) {
        String tid = event.getMDCPropertyMap().get("traceId");
        if (tid == null || tid.isBlank()) return "";
        return "[traceId=" + tid + "]";
    }
}
