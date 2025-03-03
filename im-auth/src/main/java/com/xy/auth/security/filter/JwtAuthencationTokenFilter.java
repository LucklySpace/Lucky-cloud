package com.xy.auth.security.filter;


import com.xy.auth.security.SecurityProperties;
import com.xy.auth.security.exception.TokenIsInvalidException;
import com.xy.auth.security.exception.TokenIsNullException;
import com.xy.auth.security.handle.AuthenticationFailHandler;
import com.xy.imcore.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


public class JwtAuthencationTokenFilter extends OncePerRequestFilter {


    private final String AUTH_TOKEN = "Authorization";
    private final String Bearer = "Bearer ";
    /**
     * 用户校验失败处理器
     */
    private final AuthenticationFailHandler authenticationFailHandler;
    /**
     * 忽略url地址
     */
    private final SecurityProperties securityProperties;


    public JwtAuthencationTokenFilter(AuthenticationFailHandler authenticationFailHandler, SecurityProperties securityProperties) {

        this.authenticationFailHandler = authenticationFailHandler;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 首先拿到token字符串，当用户发送非认证请求时，规定这个token字符串是放在请求头过来的
        String token = request.getHeader(AUTH_TOKEN);

        if (StringUtils.hasText(token)) {
            token = token.replaceFirst(Bearer, "");
        }

        // 校验这个token是否为null
        if (!StringUtils.hasText(token) || ObjectUtils.isEmpty(token) || "null".equals(token)) {

            //获取url地址
            String uri = request.getRequestURI();

            // 根据请求地址判断是否放行
            if (judgeIgnoreUrl(uri)) {

                filterChain.doFilter(request, response);

            } else {
                // 进入用户校验失败处理器
                authenticationFailHandler.onAuthenticationFailure(request, response, new TokenIsNullException("Token is Null"));

            }

            return;
        }

        if (JwtUtil.validate(token)) {

            try {
                //从token中获取用户名
                String usernameFromToken = JwtUtil.getUsername(token);

                //根据用户名获取用户信息
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(usernameFromToken, null, null);

                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 装入security 容器中
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                filterChain.doFilter(request, response);

            } catch (Exception e) {

                authenticationFailHandler.onAuthenticationFailure(request, response, new TokenIsInvalidException("Token is Invalid!"));
            }


        } else {

            // 进入用户校验失败处理器
            authenticationFailHandler.onAuthenticationFailure(request, response, new TokenIsInvalidException("Token is Invalid!"));

        }

    }

    /**
     * //判断忽略URL集合内是否包含请求url
     *
     * @param IgnoreUrl 请求地址
     * @return true 存在  false不存在
     */
    public boolean judgeIgnoreUrl(String IgnoreUrl) {
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
