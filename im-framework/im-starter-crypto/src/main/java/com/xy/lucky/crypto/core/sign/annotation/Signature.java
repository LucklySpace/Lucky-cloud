package com.xy.lucky.crypto.core.sign.annotation;

import com.xy.lucky.crypto.core.sign.domain.SignatureMode;

import java.lang.annotation.*;

/**
 * 接口签名/验签注解
 * <p>
 * 支持方法级别和类级别使用
 * <p>
 * 使用示例：
 * <pre>
 * // 验证请求签名
 * &#64;Signature(verify = true)
 * &#64;PostMapping("/order")
 * public Result createOrder(@RequestBody Order order) { ... }
 *
 * // 对响应进行签名
 * &#64;Signature(sign = true)
 * &#64;GetMapping("/info")
 * public Map<String, Object> getInfo() { ... }
 *
 * // 同时验签和加签
 * &#64;Signature(verify = true, sign = true)
 *
 * // 类级别配置
 * &#64;Signature(verify = true)
 * &#64;RestController
 * public class SecureController { ... }
 * </pre>
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Signature {

    /**
     * 是否验证请求签名
     */
    boolean verify() default true;

    /**
     * 是否对响应进行签名
     */
    boolean sign() default false;

    /**
     * 签名算法
     */
    SignatureMode mode() default SignatureMode.HMAC_SHA256;

    /**
     * 是否验证时间戳
     * <p>
     * 启用后会检查请求中的 timestamp 参数是否在容差范围内
     */
    boolean checkTimestamp() default true;

    /**
     * 是否启用 nonce 防重放
     */
    boolean checkNonce() default true;

    /**
     * 签名失败是否抛出异常
     */
    boolean throwOnFailure() default true;

    /**
     * 参与签名的字段（默认全部）
     * <p>
     * 为空时所有非空字段都参与签名
     */
    String[] includeFields() default {};

    /**
     * 不参与签名的字段
     */
    String[] excludeFields() default {"sign"};
}
