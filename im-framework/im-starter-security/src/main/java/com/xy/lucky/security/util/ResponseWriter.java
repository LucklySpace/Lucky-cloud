package com.xy.lucky.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public final class ResponseWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseWriter() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    public static void renderJson(HttpServletResponse response, Object payload) {
        renderJson(response, HttpServletResponse.SC_OK, payload);
    }

    public static void renderJson(HttpServletResponse response, int status, Object payload) {
        try {
            response.setStatus(status);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            log.error("写入响应失败", e);
        }
    }
}

