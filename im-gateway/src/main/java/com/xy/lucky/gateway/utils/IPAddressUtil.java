package com.xy.lucky.gateway.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Objects;


@Slf4j
public class IPAddressUtil {

    private static final String IP_UTILS_FLAG = ",";
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IP1 = "127.0.0.1";

    public static String getIPAddress(ServerHttpRequest request) {
        String ip = "";

        // 依次从不同的请求头中获取IP地址，并按优先级设置到ip变量中
        ip = getHeaderIP(request, "X-Original-Forwarded-For");
        if (!isValidIP(ip)) {
            ip = getHeaderIP(request, "X-Forwarded-For");
        }
        if (!isValidIP(ip)) {
            ip = getHeaderIP(request, "x-forwarded-for");
        }
        if (!isValidIP(ip)) {
            ip = getHeaderIP(request, "Proxy-Client-IP");
        }
        if (!isValidIP(ip)) {
            ip = getHeaderIP(request, "WL-Proxy-Client-IP");
        }
        if (!isValidIP(ip)) {
            ip = getHeaderIP(request, "HTTP_CLIENT_IP");
        }
        if (!isValidIP(ip)) {
            ip = getHeaderIP(request, "HTTP_X_FORWARDED_FOR");
        }

        // 兼容k8s集群获取ip
        if (!isValidIP(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
            if (LOCALHOST_IP1.equalsIgnoreCase(ip) || LOCALHOST_IP.equalsIgnoreCase(ip)) {
                // 根据网卡取本机配置的IP
                try {
                    InetAddress iNet = InetAddress.getLocalHost();
                    ip = iNet.getHostAddress();
                } catch (UnknownHostException e) {
                    log.error("getClientIp error: {}", e);
                }
            }
        }

        // 使用代理，则获取第一个IP地址
        int index = ip.indexOf(IP_UTILS_FLAG);
        if (index > 0) {
            ip = ip.substring(0, index);
        }

        return ip;
    }

    private static String getHeaderIP(ServerHttpRequest request, String headerName) {
        String ip = Objects.requireNonNullElse(request.getHeaders().getFirst(headerName), "");
        return isValidIP(ip) ? ip : "";
    }

    private static boolean isValidIP(String ip) {
        return StringUtils.hasLength(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }


    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                // 该网卡接口下的ip会有多个，也需要一个个的遍历，找到自己所需要的
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    // 排除loopback回环类型地址（不管是IPv4还是IPv6 只要是回环地址都会返回true）
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了 就是我们要找的
                            // ~~~~~~~~~~~~~绝大部分情况下都会在此处返回你的ip地址值~~~~~~~~~~~~~
                            return inetAddr;
                        }

                        // 若不是site-local地址 那就记录下该地址当作候选
                        if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }

                    }
                }
            }

            // 如果出去loopback回环地之外无其它地址了，那就回退到原始方案吧
            return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
