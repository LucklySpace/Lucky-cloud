package com.xy.lucky.crypto.core.crypto.annotation;

import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;

import java.lang.annotation.*;

/**
 * 接口加解密注解
 * <p>
 * 支持方法级别和类级别使用，方法级别优先级更高
 * <p>
 * 使用示例：
 * <pre>
 * // 方法级别
 * &#64;Crypto(encrypt = CryptoMode.AES, decrypt = CryptoMode.AES)
 * public Result&lt;User&gt; getUser(@RequestBody UserQuery query) { ... }
 *
 * // 类级别（对该Controller所有接口生效）
 * &#64;Crypto(encrypt = CryptoMode.AES)
 * &#64;RestController
 * public class UserController { ... }
 *
 * // 只加密响应
 * &#64;Crypto(encrypt = CryptoMode.AES)
 *
 * // 只解密请求
 * &#64;Crypto(decrypt = CryptoMode.AES)
 *
 * // 使用全局配置
 * &#64;Crypto(encrypt = CryptoMode.GLOBAL, decrypt = CryptoMode.GLOBAL)
 * </pre>
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Crypto {

    /**
     * 响应数据加密方式
     * <ul>
     *   <li>NONE - 不加密</li>
     *   <li>GLOBAL - 使用配置文件默认设置</li>
     *   <li>AES - AES对称加密</li>
     *   <li>RSA - RSA非对称加密</li>
     *   <li>SM4 - SM4国密加密</li>
     * </ul>
     */
    CryptoMode encrypt() default CryptoMode.NONE;

    /**
     * 请求数据解密方式
     */
    CryptoMode decrypt() default CryptoMode.NONE;

    /**
     * 解密/加密失败是否抛出异常
     * <p>
     * false: 失败时回退为原始数据
     * true: 失败时抛出异常
     */
    boolean throwOnFailure() default true;

    /**
     * 排除的响应字段（不加密这些字段）
     * <p>
     * 仅在响应为对象类型时生效
     * 例如: excludeFields = {"code", "message"} 则只加密 data 字段
     */
    String[] excludeEncryptFields() default {};

    /**
     * 包含的响应字段（只加密这些字段）
     * <p>
     * 优先级高于 excludeEncryptFields
     */
    String[] includeEncryptFields() default {};
}
