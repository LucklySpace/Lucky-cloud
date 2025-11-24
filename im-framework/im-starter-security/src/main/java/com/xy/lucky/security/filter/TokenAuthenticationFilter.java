package com.xy.lucky.security.filter;

import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.SecurityAuthProperties;
import com.xy.lucky.security.exception.AuthenticationFailException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public final class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SecurityAuthProperties securityAuthProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (isIgnoredUrl(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (!StringUtils.hasText(token)) {
            log.debug("Request [{}] missing token", uri);
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        try {
            if (!JwtUtil.validate(token)) {
                log.debug("Invalid token for request [{}]", uri);
                throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
            }

            String username = JwtUtil.getUsername(token);

            if (!StringUtils.hasText(username)) {
                log.debug("Token contains no username for request [{}]", uri);
                throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (AuthenticationFailException ex) {
            SecurityContextHolder.clearContext();
            throw ex;
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.error("Unexpected error while authenticating request [{}]: {}", uri, ex.getMessage(), ex);
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(IMConstant.AUTH_TOKEN_HEADER);
        if (StringUtils.hasText(header)) {
            return stripBearer(header);
        }
        String param = request.getParameter(IMConstant.ACCESS_TOKEN_PARAM);
        return StringUtils.hasText(param) ? stripBearer(param) : null;
    }

    private String stripBearer(String raw) {
        if (raw == null) return null;
        return raw.startsWith(securityAuthProperties.getHeader()) ?
                raw.substring(securityAuthProperties.getHeader().length()).trim() : raw.trim();
    }

    public boolean isIgnoredUrl(String requestUri) {
        if (!StringUtils.hasText(requestUri)) return false;

        String[] ignoreUrls = securityAuthProperties == null ? null : securityAuthProperties.getIgnore();
        if (ignoreUrls == null || ignoreUrls.length == 0) return false;

        return Arrays.stream(ignoreUrls)
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> PATH_MATCHER.match(pattern.trim(), requestUri));
    }
}

