package com.xy.auth.security.handle;


import com.xy.auth.utils.ResponseUtil;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.utils.JacksonUtil;
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

        ResponseUtil.renderString(response, JacksonUtil.toJSONString(Result.failed(ResultCode.UNAUTHORIZED)));

    }
}
