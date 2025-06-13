package com.xy.security.crypto.advice;


import com.xy.security.CryptoProperties;
import com.xy.security.crypto.annotation.Crypto;
import com.xy.security.crypto.core.CryptoExecutor;
import com.xy.security.crypto.domain.CryptoMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 请求体解密拦截器
 */
@ControllerAdvice
public class DecryptRequestBodyAdvice implements RequestBodyAdvice {

    @Autowired
    private CryptoProperties properties;

    @Autowired
    private CryptoExecutor executor;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Crypto crypto = methodParameter.getMethodAnnotation(Crypto.class);
        return crypto != null && resolveMode(crypto.decrypt(), properties.getCrypto().getDefaultDecrypt()) != CryptoMode.NONE;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        String body = new BufferedReader(new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        Crypto crypto = parameter.getMethodAnnotation(Crypto.class);
        CryptoMode mode = resolveMode(crypto.decrypt(), properties.getCrypto().getDefaultDecrypt());

        try {
            String decrypted = executor.decrypt(body, mode);
            ByteArrayInputStream bais = new ByteArrayInputStream(decrypted.getBytes(StandardCharsets.UTF_8));
            return new HttpInputMessage() {
                @Override
                public InputStream getBody() {
                    return bais;
                }

                @Override
                public HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }
            };
        } catch (Exception e) {
            throw new IOException("解密失败: " + e.getMessage(), e);
        }
    }

    private CryptoMode resolveMode(CryptoMode annMode, CryptoMode defaultMode) {
        return annMode == CryptoMode.GLOBAL ? defaultMode : annMode;
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
}