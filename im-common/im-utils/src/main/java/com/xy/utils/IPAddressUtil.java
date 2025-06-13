package com.xy.utils;


import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP地址工具类
 * 功能：
 * 1. 从HttpServletRequest中获取客户端真实IP地址，支持多种代理场景
 * 2. 作为Logback的转换器，提供本地主机IP地址
 */
@Slf4j
public class IPAddressUtil extends ClassicConverter {

    // 常量定义
    private static final String IP_UTILS_FLAG = ",";  // IP地址分隔符
    private static final String UNKNOWN = "unknown"; // 未知IP标识
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1"; // IPv6本地地址
    private static final String LOCALHOST_IPV4 = "127.0.0.1";       // IPv4本地地址

    // 常见的代理服务器IP头字段
    private static final String[] PROXY_HEADERS = {
            "X-Original-Forwarded-For", // k8s环境中真实客户端IP
            "X-Forwarded-For",          // 标准代理IP头
            "x-forwarded-for",          // 小写形式代理IP头
            "Proxy-Client-IP",          // Apache代理IP
            "WL-Proxy-Client-IP",       // WebLogic代理IP
            "HTTP_CLIENT_IP",           // 其他代理IP
            "HTTP_X_FORWARDED_FOR"     // 其他代理IP
    };

    /**
     * 获取客户端真实IP地址
     * <p>
     * 支持多种代理场景：
     * 1. 直接访问
     * 2. 通过Nginx等反向代理访问
     * 3. 在k8s集群环境中访问
     * 4. 多级代理场景
     *
     * @param request HttpServletRequest对象
     * @return 客户端真实IP地址，如果获取失败返回null
     */
    public static String getIPAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = null;

        try {
            // 遍历所有可能的代理头字段，获取第一个有效的IP地址
            for (String header : PROXY_HEADERS) {
                ip = request.getHeader(header);
                if (isValidIp(ip)) {
                    break;
                }
            }

            // 如果通过代理头未获取到有效IP，则使用远程地址
            if (!isValidIp(ip)) {
                ip = request.getRemoteAddr();

                // 处理本地地址情况，获取本机实际IP
                if (isLocalAddress(ip)) {
                    try {
                        ip = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        log.error("Failed to get local host address", e);
                    }
                }
            }

            // 处理多个IP的情况（如x-forwarded-for可能有多个IP）
            if (ip != null && ip.contains(IP_UTILS_FLAG)) {
                ip = ip.substring(0, ip.indexOf(IP_UTILS_FLAG)).trim();
            }

        } catch (Exception e) {
            log.error("Error while getting client IP address", e);
        }

        return ip;
    }

    /**
     * 判断IP地址是否有效
     *
     * @param ip IP地址
     * @return 如果IP有效且不是"unknown"则返回true
     */
    private static boolean isValidIp(String ip) {
        return !StringUtils.isEmpty(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 判断是否为本地地址
     *
     * @param ip IP地址
     * @return 如果是IPv4或IPv6的本地地址则返回true
     */
    private static boolean isLocalAddress(String ip) {
        return LOCALHOST_IPV4.equalsIgnoreCase(ip) || LOCALHOST_IPV6.equalsIgnoreCase(ip);
    }

    /**
     * Logback转换器实现，获取本地主机IP地址
     *
     * @param iLoggingEvent 日志事件
     * @return 本地主机IP地址，如果获取失败返回null
     */
    @Override
    public String convert(ILoggingEvent iLoggingEvent) {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Failed to get local host IP address", e);
            return null;
        }
    }
}