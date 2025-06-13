package com.xy.auth.security.filter;

import com.xy.auth.security.SecurityProperties;
import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.imcore.utils.JwtUtil;
import com.xy.response.domain.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@WebFilter
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final String AUTH_TOKEN = "Authorization";
    private final String ACCESS_TOKEN = "accessToken";
    private final String Bearer = "Bearer ";

    /**
     * 忽略url地址
     */
    private final SecurityProperties securityProperties;


    public TokenAuthenticationFilter(SecurityProperties securityProperties) {

        this.securityProperties = securityProperties;
    }

    /**
     * 判断url是否与规则配置:
     * ? 表示单个字符
     * * 表示一层路径内的任意字符串，不可跨层级
     * ** 表示任意层路径
     *
     * @param url     匹配规则
     * @param urlPath 需要匹配的url
     * @return
     */
    public static boolean isMatch(String url, String urlPath) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(url, urlPath);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. 如果是忽略地址，直接放行
        if (isIgnoredUrl(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 获取并校验 Token
        String token = getToken(request);
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        if (!JwtUtil.validate(token)) {
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
        }

        try {
            String username = JwtUtil.getUsername(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
    }


    /**
     * 从请求中获取token
     *
     * @param servletRequest 请求对象
     * @return 获取到的token值 可以为null
     */
    private String getToken(HttpServletRequest servletRequest) {
        //先从请求头中获取
        String headerToken = servletRequest.getHeader(AUTH_TOKEN);
        if (StringUtils.hasText(headerToken)) {
            headerToken = headerToken.replaceFirst(Bearer, "");
            return headerToken.trim();
        }
        //再从请求参数里获取
        String paramToken = servletRequest.getParameter(ACCESS_TOKEN);
        if (StringUtils.hasText(paramToken)) {
            paramToken = paramToken.replaceFirst(Bearer, "");
            return paramToken.trim();
        }
        return null;
    }

    /**
     * //判断忽略URL集合内是否包含请求url
     *
     * @param IgnoreUrl 请求地址
     * @return true 存在  false不存在
     */
    public boolean isIgnoredUrl(String IgnoreUrl) {
        //application.yml中忽略请求地址
        String[] ignoreUrls = securityProperties.getIgnoreUrl();

        if (!StringUtils.hasText(IgnoreUrl) || ignoreUrls.length == 0) {
            return false;
        }
        for (String url : ignoreUrls) {
            if (isMatch(url, IgnoreUrl)) {
                return true;
            }
        }
        return false;
    }

}
