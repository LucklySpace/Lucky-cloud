package com.xy.auth.security.handle;


import cn.hutool.core.date.DateField;
import com.xy.auth.utils.ResponseUtils;
import com.xy.imcore.model.IMessageWrap;
import com.xy.imcore.utils.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录成功
 */
//@Schema(description = "登录成功回调")
@Component
public class AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {


    @Override
    //@Schema(description = "登录成功回调")
    //@SystemLog(about = "登录系统", type = LogType.LOGIN)
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        String userId = (String) authentication.getPrincipal();

        Map<String, Object> map = new HashMap<>();

        //1.生成token
        String token = JwtUtil.createToken(userId, 24, DateField.HOUR);

        map.put("token", token);

        //2.设置token过期时间
//        long expireTime = jwtTokenUtil.getExpiredDateFromToken(token).getTime();
//        map.put("expireTime", expireTime);

        //3.设置用户名
        map.put("username", userId);

        //4.返回token给前端
        ResponseUtils.out(response, new IMessageWrap(200, map));
    }
}
