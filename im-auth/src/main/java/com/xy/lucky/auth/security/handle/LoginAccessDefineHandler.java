package com.xy.lucky.auth.security.handle;


import com.xy.lucky.auth.utils.ResponseUtil;
import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.utils.json.JacksonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 权限不足处理器
 */
@Component
public class LoginAccessDefineHandler implements AccessDeniedHandler {


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        ResponseUtil.renderString(response, JacksonUtils.toJSONString(Result.failed(ResultCode.UNAUTHORIZED)));
    }
}
