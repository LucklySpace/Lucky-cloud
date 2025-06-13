package com.xy.auth.security;


import com.xy.auth.utils.JsonUtil;
import com.xy.response.domain.Result;
import com.xy.response.domain.ResultCode;
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
        Result<?> result = Result.failed(ResultCode.CREDENTIALS_EXPIRED);
        response.setContentType("text/json;charset=utf-8");
        response.getWriter().write(Objects.requireNonNull(JsonUtil.toJSONString(result)));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Result<?> result = Result.failed(ResultCode.NO_PERMISSION);
        response.setContentType("text/json;charset=utf-8");
        response.getWriter().write(Objects.requireNonNull(JsonUtil.toJSONString(result)));
    }
}

