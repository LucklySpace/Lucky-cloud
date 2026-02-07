package com.xy.lucky.auth.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求上下文工具类
 */
public final class RequestContextUtil {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_DEVICE_ID = "X-Device-Id";
    private static final String HEADER_USER_AGENT = "User-Agent";

    private RequestContextUtil() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader(HEADER_X_REAL_IP);
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String resolveDeviceId(HttpServletRequest request, String clientIp) {
        String deviceId = request.getHeader(HEADER_DEVICE_ID);
        if (StringUtils.hasText(deviceId)) {
            return deviceId.trim();
        }
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        String raw = (StringUtils.hasText(userAgent) ? userAgent : "") + "|" + (StringUtils.hasText(clientIp) ? clientIp : "");
        return DigestUtils.sha256Hex(raw);
    }

    public static HttpServletRequest getRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
