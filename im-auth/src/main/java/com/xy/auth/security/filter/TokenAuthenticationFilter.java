package com.xy.auth.security.filter;

import com.xy.auth.security.SecurityProperties;
import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.core.constants.IMConstant;
import com.xy.core.utils.JwtUtil;
import com.xy.general.response.domain.ResultCode;
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

    /**
     * 存储忽略的URL配置
     */
    private final SecurityProperties securityProperties;

    public TokenAuthenticationFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 判断请求的 URL 是否与忽略规则匹配
     * ? 表示单个字符
     * * 表示一层路径内的任意字符串
     * ** 表示任意层路径
     *
     * @param url     匹配规则
     * @param urlPath 需要匹配的url
     * @return 是否匹配
     */
    public static boolean isMatch(String url, String urlPath) {
        // 使用 AntPathMatcher 匹配 URL 模式
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(url, urlPath);
    }

    /**
     * 校验用户token
     *
     * @param request     请求
     * @param response    返回
     * @param filterChain 过滤链
     * @throws ServletException servlet 异常
     * @throws IOException      io 异常
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 获取请求地址
        String uri = request.getRequestURI();

        // 1. 如果是忽略地址，直接放行
        if (isIgnoredUrl(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 获取并校验 Token
        String token = getToken(request);

        // 3. 判断 token 是否为空
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        // 4. 验证 token 是否有效
        if (!JwtUtil.validate(token)) {
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
        }

        try {
            // 5. 从 token 中提取用户名
            String username = JwtUtil.getUsername(token);

            // 6. 创建认证对象并设置到安全上下文
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 7. 将认证对象设置到上下文中，供后续请求使用
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 8. 继续执行过滤器链
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 发生异常时，抛出认证失败异常
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }

    }

    /**
     * 从请求中获取 token
     *
     * @param servletRequest 请求对象
     * @return 获取到的 token 值，如果没有则返回 null
     */
    private String getToken(HttpServletRequest servletRequest) {
        // 优先从请求头中获取 token
        String headerToken = servletRequest.getHeader(IMConstant.AUTH_TOKEN_HEADER);

        if (StringUtils.hasText(headerToken)) {
            return headerToken.replaceFirst(IMConstant.BEARER_PREFIX, "").trim();
        }

        // 如果请求头没有，则从请求参数中获取 token
        String paramToken = servletRequest.getParameter(IMConstant.ACCESS_TOKEN_PARAM);
        if (StringUtils.hasText(paramToken)) {
            return paramToken.replaceFirst(IMConstant.BEARER_PREFIX, "").trim();
        }

        // 如果两者都没有，则返回 null
        return null;
    }

    /**
     * 判断请求 URL 是否在忽略的 URL 列表中
     *
     * @param ignoreUrl 请求的 URL
     * @return true: 忽略该请求, false: 不忽略
     */
    public boolean isIgnoredUrl(String ignoreUrl) {
        // 获取配置中的忽略 URL 列表
        String[] ignoreUrls = securityProperties.getIgnore();

        // 如果没有配置忽略 URL，或者传入的 URL 不符合任何规则，则返回 false
        if (!StringUtils.hasText(ignoreUrl) || ignoreUrls.length == 0) {
            return false;
        }

        // 检查传入的 URL 是否符合忽略规则
        for (String url : ignoreUrls) {
            if (isMatch(url, ignoreUrl)) {
                return true;
            }
        }
        return false;
    }
}
