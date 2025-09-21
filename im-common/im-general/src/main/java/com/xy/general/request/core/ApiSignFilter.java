//package com.xy.request.core;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.xy.request.utils.SignUtils;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.stereotype.Component;
//import org.springframework.web.util.ContentCachingRequestWrapper;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
/// **
// * 签名验证过滤器：用于统一验证接口请求的签名合法性，防止伪造和重放攻击。
// * 建议仅对部分接口生效（如 /api/**），可结合 URL 路径或注解处理。
// */
//@Slf4j
//@Component
//public class ApiSignFilter implements Filter {
//
//    private static final long EXPIRE_TIME = 5 * 60; // 签名有效期：5分钟
//
//    @Override
//    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
//        HttpServletRequest request = (HttpServletRequest) req;
//        HttpServletResponse response = (HttpServletResponse) res;
//
/// /        // 非 JSON 请求或非 POST 请求跳过验证（你也可以基于 URL 控制是否校验）
/// /        if (!"POST".equalsIgnoreCase(request.getMethod())
/// /                || request.getContentType() == null
/// /                || !request.getContentType().toLowerCase().contains("application/json")) {
/// /            chain.doFilter(req, res);
/// /            return;
/// /        }
//
//        // 只拦截指定路径（例如 /api/ 开头的）
//        String uri = request.getRequestURI();
//        if (!uri.startsWith("/api/")) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        // 包装请求，缓存请求体，便于后续重复读取
//        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
//
//        // 统一收集参数（Query + Body）
//        Map<String, Object> allParams = collectAllParams(cachedRequest);
//
//        // 提取核心参数
//        String appId = String.valueOf(allParams.get("appId"));
//        String sign = String.valueOf(allParams.get("sign"));
//        String nonce = String.valueOf(allParams.get("nonce"));
//        String timestamp = String.valueOf(allParams.get("timestamp"));
//
//        // 参数完整性校验
//        if (StringUtils.isAnyBlank(appId, sign, nonce, timestamp)) {
//            throw new ApiException("签名参数不完整");
//        }
//
//        // 请求时间戳是否超出有效范围
//        long now = System.currentTimeMillis() / 1000;
//        if (Math.abs(now - Long.parseLong(timestamp)) > EXPIRE_TIME) {
//            throw new ApiException("请求时间已过期");
//        }
//
//        // 重放攻击防护（推荐使用 Redis + nonceKey 判断是否重复）
////        String nonceKey = "nonce_cache:" + appId + ":" + nonce;
////        if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(nonceKey, "1", EXPIRE_TIME, TimeUnit.SECONDS))) {
////            throw new ApiException("重复请求被拒绝");
////        }
//
//        // 本地计算签名并比对
//        String calculatedSign = SignUtils.calculateSign(allParams, getAppSecret(appId));
//        if (!sign.equalsIgnoreCase(calculatedSign)) {
//            log.error("接口参数签名不一致，请求签名:{} 验证签名:{}",sign,calculatedSign);
//            throw new ApiException("签名校验失败");
//        }
//
//        log.debug("接口签名校验成功");
//
//        // 放行：使用包装过的 request 避免后续读取失败
//        chain.doFilter(cachedRequest, res);
//    }
//
//    /**
//     * 获取 App 对应的密钥（建议从数据库或配置中心动态获取）
//     */
//    private String getAppSecret(String appId) {
//        return "secretFor_yourAppId";
//    }
//
//    /**
//     * 收集请求参数：包括 query 参数 和 JSON body 参数
//     */
//    private Map<String, Object> collectAllParams(HttpServletRequest request) throws IOException {
//        Map<String, Object> paramMap = new HashMap<>();
//
//        // 1. 读取 URL 参数或表单参数
//        request.getParameterMap().forEach((key, values) -> {
//            if (values.length > 0) {
//                paramMap.put(key, values[0]);
//            }
//        });
//
//        // 2. 读取 JSON 请求体参数
//        if ("POST".equalsIgnoreCase(request.getMethod())
//                && request.getContentType() != null
//                && request.getContentType().toLowerCase().contains("application/json")) {
//
//            String jsonBody = getRequestBody(request);
//            if (StringUtils.isNotBlank(jsonBody)) {
//                ObjectMapper mapper = new ObjectMapper();
//                Map<String, Object> jsonMap = mapper.readValue(jsonBody, Map.class);
//                paramMap.putAll(jsonMap);
//            }
//        }
//
//        return paramMap;
//    }
//
//    /**
//     * 从请求体中读取 JSON 字符串
//     */
//    private String getRequestBody(HttpServletRequest request) throws IOException {
//        BufferedReader reader = request.getReader();
//        StringBuilder sb = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) sb.append(line);
//        return sb.toString();
//    }
//}