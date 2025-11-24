package com.xy.lucky.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

public class ResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void renderJson(HttpServletResponse response, Object payload) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(OBJECT_MAPPER.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
    }
}

