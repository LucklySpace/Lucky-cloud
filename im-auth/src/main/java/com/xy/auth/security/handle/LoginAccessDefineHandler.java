package com.xy.auth.security.handle;

import com.xy.auth.utils.JsonUtil;
import com.xy.auth.utils.ResponseUtils;
import com.xy.imcore.model.IMessageWrap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限不足处理器
 */
@Component
public class LoginAccessDefineHandler implements AccessDeniedHandler {


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {

        IMessageWrap wrap = new IMessageWrap(700, "您没有开通对应的权限，请联系管理员！");

        ResponseUtils.out(response, wrap);
    }
}
