package com.xy.lucky.crypto.core.crypto.annotation;

import java.lang.annotation.*;

/**
 * 忽略加解密注解
 * <p>
 * 当类上标注了 @Crypto 注解时，可以使用此注解排除特定方法
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Crypto(encrypt = CryptoMode.AES)
 * &#64;RestController
 * public class UserController {
 *
 *     // 此方法会加密响应
 *     &#64;GetMapping("/info")
 *     public User getInfo() { ... }
 *
 *     // 此方法不加密
 *     &#64;IgnoreCrypto
 *     &#64;GetMapping("/public")
 *     public User getPublicInfo() { ... }
 * }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreCrypto {

    /**
     * 是否忽略加密
     */
    boolean ignoreEncrypt() default true;

    /**
     * 是否忽略解密
     */
    boolean ignoreDecrypt() default true;
}

