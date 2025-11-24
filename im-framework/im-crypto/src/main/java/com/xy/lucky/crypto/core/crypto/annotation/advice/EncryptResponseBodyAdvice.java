package com.xy.lucky.crypto.core.crypto.annotation.advice;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.annotation.Crypto;
import com.xy.lucky.crypto.core.crypto.core.CryptoExecutor;
import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import jakarta.annotation.Resource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体加密拦截器
 * 将控制器返回对象序列化为 JSON 后进行加密，返回密文字符串。
 * 加密模式优先使用方法上的注解设置，未显式指定时采用配置中的默认模式。
 */
@ControllerAdvice
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Resource
    private CryptoProperties properties;

    @Resource
    private CryptoExecutor executor;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Crypto crypto = returnType.getMethodAnnotation(Crypto.class);
        return crypto != null && resolveMode(crypto.encrypt(), properties.getCrypto().getDefaultEncrypt()) != CryptoMode.NONE;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        Crypto crypto = returnType.getMethodAnnotation(Crypto.class);
        CryptoMode mode = resolveMode(crypto.encrypt(), properties.getCrypto().getDefaultEncrypt());
        boolean throwOnFailure = crypto.throwOnFailure();
        try {
            // 将响应对象序列化为 JSON 后加密。
            String payload;
            if (body == null) {
                payload = "";
            } else if (body instanceof CharSequence) {
                payload = body.toString();
            } else {
                payload = objectMapper.writeValueAsString(body);
            }
            return executor.encrypt(payload, mode);
        } catch (Exception e) {
            if (throwOnFailure) {
                throw new IllegalStateException("响应加密失败: " + e.getMessage(), e);
            }
            // 加密失败且不抛出异常时，回退为原始响应。
            return body;
        }
    }

    private CryptoMode resolveMode(CryptoMode annMode, CryptoMode defaultMode) {
        return annMode == CryptoMode.GLOBAL ? defaultMode : annMode;
    }
}
