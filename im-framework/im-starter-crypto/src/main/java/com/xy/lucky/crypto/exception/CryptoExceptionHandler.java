package com.xy.lucky.crypto.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 加解密模块统一异常处理
 * <p>
 * 可通过 @Order 调整优先级，或在业务代码中自定义异常处理覆盖此处理器
 */
@Slf4j
@Order(100)
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CryptoExceptionHandler {

    /**
     * 处理加解密异常
     */
    @ExceptionHandler(CryptoException.class)
    public ResponseEntity<Map<String, Object>> handleCryptoException(CryptoException e) {
        log.error("加解密异常: {}", e.getMessage());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", "数据处理失败");
        result.put("error", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理签名异常
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureException(SignatureException e) {
        log.error("签名验证异常: {} - {}", e.getErrorCode(), e.getMessage());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", "签名验证失败");
        result.put("error", e.getErrorCode().name());
        result.put("detail", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }
}

