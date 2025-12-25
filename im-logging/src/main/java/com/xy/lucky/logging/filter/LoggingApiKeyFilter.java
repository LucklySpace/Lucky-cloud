package com.xy.lucky.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoggingApiKeyFilter extends OncePerRequestFilter {
    @Value("${logging.api-key:}")
    private String apiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean protect = uri.startsWith("/api/logs");
        if (protect && apiKey != null && !apiKey.isBlank()) {
            String header = request.getHeader("X-Api-Key");
            if (header == null || !header.equals(apiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("unauthorized");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
