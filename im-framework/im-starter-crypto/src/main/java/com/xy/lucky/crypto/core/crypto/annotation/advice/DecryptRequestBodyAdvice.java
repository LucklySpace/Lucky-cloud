package com.xy.lucky.crypto.core.crypto.annotation.advice;


import com.xy.lucky.crypto.CryptoProperties;
import com.xy.lucky.crypto.core.crypto.annotation.Crypto;
import com.xy.lucky.crypto.core.crypto.core.CryptoExecutor;
import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import jakarta.annotation.Resource;
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
 * 将请求体按 UTF-8 读取为字符串，根据解密模式解密后再交由后续转换器处理。
 * 解密模式优先使用方法上的注解设置，未显式指定时采用配置中的默认模式。
 */
@ControllerAdvice
public class DecryptRequestBodyAdvice implements RequestBodyAdvice {

    @Resource
    private CryptoProperties properties;

    @Resource
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
        boolean throwOnFailure = crypto.throwOnFailure();

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
            if (throwOnFailure) {
                throw new IOException("解密失败: " + e.getMessage(), e);
            }
            // 解密失败且不抛异常时，回退为原始数据。
            return inputMessage;
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
