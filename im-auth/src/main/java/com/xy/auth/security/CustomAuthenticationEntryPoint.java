package com.xy.auth.security;


import com.xy.auth.response.Result;
import com.xy.auth.utils.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * @author dense
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Result<?> result = Result.failed(401, "用户未登录或已过期");
        response.setContentType("text/json;charset=utf-8");
        response.getWriter().write(Objects.requireNonNull(JsonUtil.toJSONString(result)));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Result<?> result = Result.failed(403, "权限不足");
        response.setContentType("text/json;charset=utf-8");
        response.getWriter().write(Objects.requireNonNull(JsonUtil.toJSONString(result)));
    }
}

