package com.xy.auth.security.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.auth.security.token.QrScanAuthenticationToken;
import com.xy.auth.security.token.SmsAuthenticationToken;
import com.xy.auth.security.token.UserAuthenticationToken;
import com.xy.auth.utils.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @description:用户名和密码登陆验证的过滤器
 * @date: 2021/3/10 15:04
 */
@Slf4j
public class UserAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    // 验证类型，比如sms ,form ,qrcode
    private static final String AUTH_TYPE_PARAMETER = "authType";

    // 对应用户名或手机号等
    private static final String PRINCIPAL_PARAMETER = "principal";

    // 对应密码或验证码等
    private static final String CREDENTIALS_PARAMETER = "credentials";

    // 登录类型 -- 表单登录
    private static final String AUTH_TYPE_FORM = "form";

    // 登录类型 -- 手机验证码登录
    private static final String AUTH_TYPE_SMS = "sms";

    // 登录类型 -- 二维码登录
    private static final String AUTH_TYPE_QRCODE = "qrcode";
    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER =
            new AntPathRequestMatcher("/user/login", "POST");
    private final boolean postOnly = true;

    // 缓存请求体
    private Map<String, String> cachedRequestBody;

    /**
     * 父类中定义了拦截的请求URL，/login的post请求，直接使用这个配置，也可以自己重写
     */
    public UserAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        if (postOnly && !request.getMethod().equalsIgnoreCase("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        // 获取所有认证参数
        Map<String, String> authParams = readRequestBody(request);

        String authType = authParams.get(AUTH_TYPE_PARAMETER);
        String principal = authParams.get(PRINCIPAL_PARAMETER);
        String credentials = authParams.get(CREDENTIALS_PARAMETER);

        // 参数合法性检查
        if (!StringUtils.hasText(authType) || !StringUtils.hasText(principal) || !StringUtils.hasText(credentials)) {
            throw new AuthenticationServiceException("Missing authentication parameters");
        }

        AbstractAuthenticationToken authRequest;

        // 根据不同的请求类型进行认证
        switch (authType) {
            case AUTH_TYPE_FORM:
                authRequest = new UserAuthenticationToken(principal, credentials);
                break;
            case AUTH_TYPE_SMS:
                authRequest = new SmsAuthenticationToken(principal, credentials);
                break;
            case AUTH_TYPE_QRCODE:
                authRequest = new QrScanAuthenticationToken(principal, credentials);
                break;
            default:
                throw new AuthenticationServiceException("Unsupported authentication type: " + authType);
        }

        // 设置认证请求的详情信息
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));

        // 调用 AuthenticationManager 进行认证
        return this.getAuthenticationManager().authenticate(authRequest);
    }



    /**
     * 获取认证请求的所有参数 读取请求体并返回一个Map
     * @param request Http请求对象
     * @return 请求体中包含的参数Map
     * @throws Exception 如果读取时出错
     */
    @SneakyThrows
    private Map<String, String> readRequestBody(HttpServletRequest request)  {
        String body = request.getReader().lines().collect(Collectors.joining());
        if (StringUtils.hasText(body)) {
            // 如果请求体非空，解析成Map
            return JsonUtil.parseObject(body, HashMap.class);
        } else {
            // 如果请求体为空，解析URL参数
            Map<String, String> parameterMap = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    parameterMap.put(key, values[0]);
                }
            });
            return parameterMap;
        }
    }


}

