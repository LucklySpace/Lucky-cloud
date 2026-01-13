package com.xy.lucky.crypto.core.sign.annotation.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.sign.annotation.Signature;
import com.xy.lucky.crypto.core.sign.cache.NonceCache;
import com.xy.lucky.crypto.core.sign.core.SignatureAlgorithm;
import com.xy.lucky.crypto.core.sign.domain.SignatureMode;
import com.xy.lucky.crypto.core.sign.utils.SignUtil;
import com.xy.lucky.crypto.exception.SignatureException;
import com.xy.lucky.crypto.exception.SignatureException.SignatureErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 签名验签切面
 * <p>
 * 特性：
 * - 支持 URL 参数和 JSON Body 签名
 * - 支持时间戳验证
 * - 支持 nonce 防重放
 * - 支持多种签名算法
 * - 支持类级别和方法级别注解
 */
@Slf4j
@Aspect
@Component
public class SignatureAspect {

    private final Map<SignatureMode, SignatureAlgorithm> algorithmMap = new EnumMap<>(SignatureMode.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    @Resource
    private CryptoProperties properties;
    @Resource
    private ObjectMapper objectMapper;
    private NonceCache nonceCache;

    @Autowired
    public SignatureAspect(List<SignatureAlgorithm> algorithms) {
        for (SignatureAlgorithm alg : algorithms) {
            algorithmMap.put(alg.mode(), alg);
        }
    }

    @PostConstruct
    public void init() {
        // 初始化 nonce 缓存
        if (properties.getSign().isNonceEnabled()) {
            nonceCache = new NonceCache(properties.getSign().getNonceExpireSeconds());
        }
    }

    @PreDestroy
    public void destroy() {
        if (nonceCache != null) {
            nonceCache.shutdown();
        }
    }

    @Around("@annotation(signature) || @within(signature)")
    public Object around(ProceedingJoinPoint pjp, Signature signature) throws Throwable {
        // 检查是否启用
        if (!properties.isEnabled() || !properties.getSign().isEnabled()) {
            return pjp.proceed();
        }

        // 获取实际的注解（方法级别优先于类级别）
        Signature actualSignature = getActualSignature(pjp, signature);
        if (actualSignature == null) {
            return pjp.proceed();
        }

        // 检查排除路径
        if (isExcludedPath()) {
            return pjp.proceed();
        }

        HttpServletRequest request = getRequest();

        // 验签逻辑
        if (actualSignature.verify()) {
            verifySignature(request, actualSignature);
        }

        // 执行方法
        Object result = pjp.proceed();

        // 响应加签
        if (actualSignature.sign() && result instanceof Map) {
            return signResponse((Map<?, ?>) result, actualSignature);
        }

        return result;
    }

    /**
     * 验证签名
     */
    private void verifySignature(HttpServletRequest request, Signature signature) throws Exception {
        CryptoProperties.Sign signConfig = properties.getSign();

        // 获取请求参数
        Map<String, String> params = extractParams(request);

        // 获取签名
        String clientSign = params.get(signConfig.getSignFieldName());
        if (!StringUtils.hasText(clientSign)) {
            handleVerifyFailure(signature, SignatureErrorCode.SIGN_MISSING);
            return;
        }

        // 验证时间戳
        if (signature.checkTimestamp()) {
            String timestampStr = params.get(signConfig.getTimestampFieldName());
            if (!StringUtils.hasText(timestampStr)) {
                handleVerifyFailure(signature, SignatureErrorCode.TIMESTAMP_MISSING);
                return;
            }
            try {
                long timestamp = Long.parseLong(timestampStr);
                if (!SignUtil.isTimestampValid(timestamp, signConfig.getTimestampTolerance())) {
                    handleVerifyFailure(signature, SignatureErrorCode.TIMESTAMP_EXPIRED);
                    return;
                }
            } catch (NumberFormatException e) {
                handleVerifyFailure(signature, SignatureErrorCode.TIMESTAMP_MISSING);
                return;
            }
        }

        // 验证 nonce
        if (signature.checkNonce() && signConfig.isNonceEnabled()) {
            String nonce = params.get(signConfig.getNonceFieldName());
            if (!StringUtils.hasText(nonce)) {
                handleVerifyFailure(signature, SignatureErrorCode.NONCE_MISSING);
                return;
            }
            if (nonceCache != null && nonceCache.checkAndSet(nonce)) {
                handleVerifyFailure(signature, SignatureErrorCode.NONCE_DUPLICATE);
                return;
            }
        }

        // 验证签名
        SignatureAlgorithm algorithm = algorithmMap.get(signature.mode());
        if (algorithm == null) {
            throw new SignatureException(SignatureErrorCode.ALGORITHM_NOT_SUPPORTED,
                    signature.mode().name());
        }

        // 移除签名字段后验签
        params.remove(signConfig.getSignFieldName());

        boolean valid = algorithm.verify(params, clientSign, signature.excludeFields());
        if (!valid) {
            handleVerifyFailure(signature, SignatureErrorCode.SIGN_INVALID);
        }

        if (properties.getCrypto().isDebug()) {
            log.debug("签名验证通过 - 路径: {}", request.getRequestURI());
        }
    }

    /**
     * 提取请求参数（支持 URL 参数和 JSON Body）
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();

        // URL 参数
        params.putAll(SignUtil.getParams(request));

        // JSON Body 参数（如果是 JSON 请求）
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                // 注意：这里需要使用可重复读取的 Request 包装器
                Map<String, String> jsonParams = SignUtil.getJsonParams(request);
                params.putAll(jsonParams);
            } catch (Exception e) {
                log.warn("解析 JSON Body 失败: {}", e.getMessage());
            }
        }

        return params;
    }

    /**
     * 对响应进行签名
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> signResponse(Map<?, ?> result, Signature signature) throws Exception {
        SignatureAlgorithm algorithm = algorithmMap.get(signature.mode());
        if (algorithm == null) {
            return (Map<Object, Object>) result;
        }

        Map<Object, Object> mutable = new LinkedHashMap<>((Map<Object, Object>) result);

        // 转换为字符串 Map 用于签名
        Map<String, String> toSign = new TreeMap<>();
        for (Map.Entry<Object, Object> e : mutable.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                toSign.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }

        // 添加时间戳
        String timestampField = properties.getSign().getTimestampFieldName();
        if (!toSign.containsKey(timestampField)) {
            long timestamp = SignUtil.generateTimestamp();
            mutable.put(timestampField, timestamp);
            toSign.put(timestampField, String.valueOf(timestamp));
        }

        // 计算签名
        String sign = algorithm.sign(toSign, signature.excludeFields());
        mutable.put(properties.getSign().getSignFieldName(), sign);

        return mutable;
    }

    /**
     * 处理验签失败
     */
    private void handleVerifyFailure(Signature signature, SignatureErrorCode errorCode) {
        if (signature.throwOnFailure()) {
            throw new SignatureException(errorCode);
        } else {
            log.warn("签名验证失败: {}", errorCode.getMessage());
        }
    }

    /**
     * 获取实际的签名注解（方法级别优先）
     */
    private Signature getActualSignature(ProceedingJoinPoint pjp, Signature classSignature) {
        try {
            MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
            Method method = methodSignature.getMethod();

            // 方法级别注解
            Signature methodAnn = method.getAnnotation(Signature.class);
            if (methodAnn != null) {
                return methodAnn;
            }

            // 类级别注解
            return classSignature != null ? classSignature :
                    pjp.getTarget().getClass().getAnnotation(Signature.class);
        } catch (Exception e) {
            return classSignature;
        }
    }

    /**
     * 检查是否排除路径
     */
    private boolean isExcludedPath() {
        String path = getRequestPath();
        if (path == null) {
            return false;
        }
        for (String excludePath : properties.getSign().getExcludePaths()) {
            if (pathMatcher.match(excludePath, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前请求路径
     */
    private String getRequestPath() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : null;
    }

    /**
     * 获取当前 HTTP 请求
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
