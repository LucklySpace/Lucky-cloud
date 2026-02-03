package com.xy.lucky.crypto.core.crypto.annotation.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.annotation.Crypto;
import com.xy.lucky.crypto.core.crypto.annotation.IgnoreCrypto;
import com.xy.lucky.crypto.core.crypto.core.CryptoExecutor;
import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import com.xy.lucky.crypto.exception.CryptoException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 响应体加密拦截器
 * <p>
 * 特性：
 * - 支持方法级别和类级别 @Crypto 注解
 * - 支持 @IgnoreCrypto 排除特定方法
 * - 支持排除路径配置
 * - 支持字段级别加密控制
 * - 注解缓存优化
 * - 自动添加加密算法 Header
 */
@Slf4j
@ControllerAdvice
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Resource
    private CryptoProperties properties;

    @Resource
    private CryptoExecutor executor;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 路径匹配器
     */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 注解缓存
     */
    private final Map<String, CryptoAnnotationInfo> annotationCache = new ConcurrentHashMap<>();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查是否启用
        if (!properties.isEnabled() || !properties.getCrypto().isEnabled()) {
            return false;
        }

        // 排除 Swagger/Knife4j/Springdoc 相关的控制器，避免干扰 API 文档
        String className = returnType.getDeclaringClass().getName();
        if (className.startsWith("org.springdoc") ||
                className.startsWith("springfox") ||
                className.startsWith("io.swagger")) {
            return false;
        }

        // 获取注解信息
        CryptoAnnotationInfo info = getAnnotationInfo(returnType);
        if (info == null || info.encryptMode == CryptoMode.NONE) {
            return false;
        }

        // 检查是否被忽略
        IgnoreCrypto ignoreCrypto = returnType.getMethodAnnotation(IgnoreCrypto.class);
        if (ignoreCrypto != null && ignoreCrypto.ignoreEncrypt()) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 检查排除路径
        if (isExcludedPath(request)) {
            return body;
        }

        CryptoAnnotationInfo info = getAnnotationInfo(returnType);
        CryptoMode mode = resolveMode(info.encryptMode, properties.getCrypto().getDefaultEncrypt());

        if (properties.getCrypto().isDebug()) {
            log.debug("响应加密 - 路径: {}, 模式: {}", request.getURI().getPath(), mode);
        }

        try {
            String payload = serializeBody(body, info);
            String encrypted = executor.encrypt(payload, mode);

            // 添加加密算法 Header
            if (properties.getCrypto().isIncludeAlgorithmHeader()) {
                response.getHeaders().add(
                        properties.getCrypto().getAlgorithmHeaderName(),
                        mode.name()
                );
            }

            return encrypted;
        } catch (Exception e) {
            log.error("响应加密失败: {}", e.getMessage());
            if (info.throwOnFailure) {
                throw new CryptoException("响应加密失败: " + e.getMessage(), e);
            }
            return body;
        }
    }

    /**
     * 序列化响应体
     */
    private String serializeBody(Object body, CryptoAnnotationInfo info) throws Exception {
        if (body == null) {
            return "";
        }
        if (body instanceof CharSequence) {
            return body.toString();
        }

        // 处理字段过滤
        if ((info.includeFields != null && info.includeFields.length > 0) ||
                (info.excludeFields != null && info.excludeFields.length > 0)) {
            return serializeWithFieldFilter(body, info);
        }

        return objectMapper.writeValueAsString(body);
    }

    /**
     * 带字段过滤的序列化
     */
    @SuppressWarnings("unchecked")
    private String serializeWithFieldFilter(Object body, CryptoAnnotationInfo info) throws Exception {
        // 转换为 Map 处理
        Map<String, Object> map = objectMapper.convertValue(body, Map.class);

        Set<String> includeSet = info.includeFields != null ?
                Arrays.stream(info.includeFields).collect(Collectors.toSet()) : null;
        Set<String> excludeSet = info.excludeFields != null ?
                Arrays.stream(info.excludeFields).collect(Collectors.toSet()) : null;

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // includeFields 优先
            if (includeSet != null && !includeSet.isEmpty()) {
                if (includeSet.contains(key)) {
                    // 只加密指定字段
                    result.put(key, encryptValue(value));
                } else {
                    result.put(key, value);
                }
            } else if (excludeSet != null && excludeSet.contains(key)) {
                // 排除字段不加密
                result.put(key, value);
            } else {
                result.put(key, value);
            }
        }

        // 如果有 includeFields，直接返回处理后的结果
        if (includeSet != null && !includeSet.isEmpty()) {
            return objectMapper.writeValueAsString(result);
        }

        // 否则整体加密
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 加密单个值
     */
    private Object encryptValue(Object value) throws Exception {
        if (value == null) {
            return null;
        }
        String strValue = value instanceof String ? (String) value : objectMapper.writeValueAsString(value);
        return executor.encrypt(strValue, properties.getCrypto().getDefaultEncrypt());
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
                        methodCrypto.throwOnFailure(),
                        methodCrypto.excludeEncryptFields(),
                        methodCrypto.includeEncryptFields()
                );
            }

            // 类级别注解
            Crypto classCrypto = parameter.getContainingClass().getAnnotation(Crypto.class);
            if (classCrypto != null) {
                return new CryptoAnnotationInfo(
                        classCrypto.encrypt(),
                        classCrypto.decrypt(),
                        classCrypto.throwOnFailure(),
                        classCrypto.excludeEncryptFields(),
                        classCrypto.includeEncryptFields()
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
    private boolean isExcludedPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        for (String excludePath : properties.getCrypto().getExcludePaths()) {
            if (pathMatcher.match(excludePath, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 注解信息缓存对象
     */
    private record CryptoAnnotationInfo(
            CryptoMode encryptMode,
            CryptoMode decryptMode,
            boolean throwOnFailure,
            String[] excludeFields,
            String[] includeFields
    ) {
    }
}
