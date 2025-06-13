package com.xy.security.crypto.advice;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.security.CryptoProperties;
import com.xy.security.crypto.annotation.Crypto;
import com.xy.security.crypto.core.CryptoExecutor;
import com.xy.security.crypto.domain.CryptoMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体加密拦截器
 */
@ControllerAdvice
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private CryptoProperties properties;

    @Autowired
    private CryptoExecutor executor;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Crypto crypto = returnType.getMethodAnnotation(Crypto.class);
        return crypto != null && resolveMode(crypto.encrypt(), properties.getCrypto().getDefaultEncrypt()) != CryptoMode.NONE;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        try {
            Crypto crypto = returnType.getMethodAnnotation(Crypto.class);
            CryptoMode mode = resolveMode(crypto.encrypt(), properties.getCrypto().getDefaultEncrypt());
            String json = new ObjectMapper().writeValueAsString(body);
            return executor.encrypt(json, mode);
        } catch (Exception e) {
            return "加密失败: " + e.getMessage();
        }
    }

    private CryptoMode resolveMode(CryptoMode annMode, CryptoMode defaultMode) {
        return annMode == CryptoMode.GLOBAL ? defaultMode : annMode;
    }
}