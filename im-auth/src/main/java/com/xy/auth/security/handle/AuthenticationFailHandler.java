package com.xy.auth.security.handle;


import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.auth.security.exception.TokenIsInvalidException;
import com.xy.auth.security.exception.TokenIsNullException;
import com.xy.auth.utils.RedisUtil;
import com.xy.auth.utils.ResponseUtils;
import com.xy.imcore.model.IMessageWrap;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RefreshScope
//@Schema(description = "登录失败回调")
public class AuthenticationFailHandler extends SimpleUrlAuthenticationFailureHandler {


    private static final String LOGIN_FAIL = "loginFailures:";


    private String LOGIN_FAIL_COUNT = "5";


    @Resource
    private RedisUtil redisUtil;


    @Override
    //@Schema(description = "登录失败回调")
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        //1.设置响应编码
        response.setContentType("application/json;charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();
        String str = null;
        int code = 500;
        if (exception instanceof AccountExpiredException) {
            str = "账户过期，登录失败!";
        } else if (exception instanceof BadCredentialsException) {
            str = "用户名或密码错误，登录失败!";
        } else if (exception instanceof CredentialsExpiredException) {
            str = "密码过期，登录失败!";
        } else if (exception instanceof DisabledException) {
            str = "账户被禁用，登录失败!";
        } else if (exception instanceof LockedException) {
            str = "账户被锁，登录失败!";
        } else if (exception instanceof InternalAuthenticationServiceException) {
            str = "账户不存在，登录失败!";
        } else if (exception instanceof TokenIsNullException) {
            str = "用户token为空，登录失败!";
        } else if (exception instanceof TokenIsInvalidException) {
            str = "用户token验证异常，登录失败!";
        } else if (exception instanceof AuthenticationFailException) {
            //token验证失败
            code = 600;
            str = exception.getMessage();
        } else {
            str = "登录失败!";
        }

        IMessageWrap wrap = new IMessageWrap(code, str);
        // 设置返回格式
        ResponseUtils.out(response, wrap);
    }
}
