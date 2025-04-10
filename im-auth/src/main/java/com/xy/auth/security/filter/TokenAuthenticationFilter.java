package com.xy.auth.security.filter;

import com.xy.auth.security.SecurityProperties;
import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.imcore.utils.JwtUtil;
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

import static com.xy.auth.response.ResultCode.*;

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
    protected void doFilterInternal(@NonNull HttpServletRequest servletRequest, @NonNull HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException, ServletException {

        // 获取token
        String token = getToken(servletRequest);

        // 如果没有token，跳过该过滤器
        if (StringUtils.hasText(token)) {

            if (JwtUtil.validate(token)) {

                try {
                    //从token中获取用户名
                    String username = JwtUtil.getUsername(token);

                    //根据用户名获取用户信息
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, null, null);

                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(servletRequest));

                    // 装入security 容器中
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                    // 进入过滤器链
                    filterChain.doFilter(servletRequest, httpServletResponse);

                } catch (Exception e) {

                    // 校验异常
                    throw new AuthenticationFailException(AUTHENTICATION_FAILED);
                }

            } else {

                //  校验过期 失效 异常
                throw new AuthenticationFailException(TOKEN_IS_INVALID);

            }

        } else {

            //获取url地址
            String uri = servletRequest.getRequestURI();

            // 根据请求地址判断是否放行
            if (judgeIgnoreUrl(uri)) {

                // 进入过滤器链
                filterChain.doFilter(servletRequest, httpServletResponse);
            } else {

                // 不是放行地址 进入用户校验失败处理器
                throw new AuthenticationFailException(TOKEN_IS_NULL);
            }
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
