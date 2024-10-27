package com.xy.auth.security.handle;

import com.xy.auth.utils.JsonUtil;
import com.xy.auth.utils.ResponseUtils;
import com.xy.imcore.model.IMessageWrap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


import java.io.IOException;

/**
 * @Description 退出成功处理器
 * @Author xy
 * @Date 2023-05-15 10:55
 */
@Component
public class LogoutSuccessHandler implements org.springframework.security.web.authentication.logout.LogoutSuccessHandler {


    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        IMessageWrap wrap = new IMessageWrap(300, "退出");

        ResponseUtils.out(response,wrap);
    }

}
