package com.xy.auth.security.handle;

import com.xy.auth.utils.JsonUtil;
import com.xy.auth.utils.ResponseUtils;
import com.xy.imcore.model.IMessageWrap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 匿名用户访问资源处理器
 */
@Component
public class LoginAuthenticationHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        IMessageWrap wrap = new IMessageWrap(600, "匿名用户没有权限进行访问！");

        ResponseUtils.out(response, wrap);
    }
}

