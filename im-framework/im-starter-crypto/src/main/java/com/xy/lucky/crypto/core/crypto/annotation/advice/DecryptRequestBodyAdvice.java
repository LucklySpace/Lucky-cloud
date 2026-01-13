package com.xy.lucky.crypto.core.crypto.annotation.advice;

import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.annotation.Crypto;
import com.xy.lucky.crypto.core.crypto.annotation.IgnoreCrypto;
import com.xy.lucky.crypto.core.crypto.core.CryptoExecutor;
import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import com.xy.lucky.crypto.exception.CryptoException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求体解密拦截器
 * <p>
 * 特性：
 * - 支持方法级别和类级别 @Crypto 注解
 * - 支持 @IgnoreCrypto 排除特定方法
 * - 支持排除路径配置
 * - 注解缓存优化
 */
@Slf4j
@ControllerAdvice
public class DecryptRequestBodyAdvice implements RequestBodyAdvice {

    @Resource
    private CryptoProperties properties;

    @Resource
    private CryptoExecutor executor;

    /**
     * 路径匹配器
     */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 注解缓存，避免重复反射
     */
    private final Map<String, CryptoAnnotationInfo> annotationCache = new ConcurrentHashMap<>();

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查是否启用
        if (!properties.isEnabled() || !properties.getCrypto().isEnabled()) {
            return false;
        }

        // 检查排除路径
        if (isExcludedPath()) {
            return false;
        }

        // 获取注解信息
        CryptoAnnotationInfo info = getAnnotationInfo(methodParameter);
        if (info == null || info.decryptMode == CryptoMode.NONE) {
            return false;
        }

        // 检查是否被忽略
        IgnoreCrypto ignoreCrypto = methodParameter.getMethodAnnotation(IgnoreCrypto.class);
        if (ignoreCrypto != null && ignoreCrypto.ignoreDecrypt()) {
            return false;
        }

        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        // 读取请求体
        byte[] bodyBytes = StreamUtils.copyToByteArray(inputMessage.getBody());
        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        if (body.isEmpty()) {
            return inputMessage;
        }

        CryptoAnnotationInfo info = getAnnotationInfo(parameter);
        CryptoMode mode = resolveMode(info.decryptMode, properties.getCrypto().getDefaultDecrypt());

        if (properties.getCrypto().isDebug()) {
            log.debug("请求解密 - 路径: {}, 模式: {}", getRequestPath(), mode);
        }

        try {
            String decrypted = executor.decrypt(body, mode);

            return new HttpInputMessage() {
                @Override
                public InputStream getBody() {
                    return new ByteArrayInputStream(decrypted.getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }
            };
        } catch (Exception e) {
            log.error("请求解密失败: {}", e.getMessage());
            if (info.throwOnFailure) {
                throw new CryptoException("请求解密失败: " + e.getMessage(), e);
            }
            // 解密失败回退原始数据
            return new HttpInputMessage() {
                @Override
                public InputStream getBody() {
                    return new ByteArrayInputStream(bodyBytes);
                }

                @Override
                public HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }
            };
        }
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /**
     * 获取注解信息（带缓存）
     */
    private CryptoAnnotationInfo getAnnotationInfo(MethodParameter parameter) {
        String cacheKey = parameter.getContainingClass().getName() + "#" + parameter.getMethod().getName();

        return annotationCache.computeIfAbsent(cacheKey, key -> {
            // 方法级别注解优先
            Crypto methodCrypto = parameter.getMethodAnnotation(Crypto.class);
            if (methodCrypto != null) {
                return new CryptoAnnotationInfo(
                        methodCrypto.encrypt(),
                        methodCrypto.decrypt(),
                        methodCrypto.throwOnFailure()
                );
            }

            // 类级别注解
            Crypto classCrypto = parameter.getContainingClass().getAnnotation(Crypto.class);
            if (classCrypto != null) {
                return new CryptoAnnotationInfo(
                        classCrypto.encrypt(),
                        classCrypto.decrypt(),
                        classCrypto.throwOnFailure()
                );
            }

            return null;
        });
    }

    /**
     * 解析加密模式
     */
    private CryptoMode resolveMode(CryptoMode annMode, CryptoMode defaultMode) {
        return annMode == CryptoMode.GLOBAL ? defaultMode : annMode;
    }

    /**
     * 检查当前请求路径是否被排除
     */
    private boolean isExcludedPath() {
        String path = getRequestPath();
        if (path == null) {
            return false;
        }
        for (String excludePath : properties.getCrypto().getExcludePaths()) {
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
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRequestURI();
        }
        return null;
    }

    /**
     * 注解信息缓存对象
     */
    private record CryptoAnnotationInfo(
            CryptoMode encryptMode,
            CryptoMode decryptMode,
            boolean throwOnFailure
    ) {
    }
}
