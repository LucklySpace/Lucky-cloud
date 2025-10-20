package com.xy.auth.security.handle;


import com.xy.auth.utils.ResponseUtil;
import com.xy.general.response.domain.Result;
import com.xy.utils.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 当未登录或者token失效访问接口时，自定义的返回结果
 */
@Component
public class LoginAuthenticationHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        ResponseUtil.renderString(response, JacksonUtil.toJSONString(Result.failed(authException.getMessage())));
    }
}

