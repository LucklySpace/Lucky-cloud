package com.xy.crypto.core.annotation;

import com.xy.crypto.core.domain.CryptoMode;

import java.lang.annotation.*;

/**
 * 统一接口加解密注解，支持通过 encrypt 和 decrypt 属性指定加解密模式。
 * 可选的加密模式包括 AES、RSA、NONE 和 GLOBAL（使用配置文件默认设置）。
 * <p>
 * 使用示例：
 *
 * @Crypto(encrypt = Crypto.CryptoMode.AES, decrypt = Crypto.CryptoMode.RSA)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Crypto {

    /**
     * 响应数据加密方式：AES、RSA、NONE、GLOBAL（默认使用配置）
     */
    CryptoMode encrypt() default CryptoMode.GLOBAL;

    /**
     * 请求数据解密方式：AES、RSA、NONE、GLOBAL（默认使用配置）
     */
    CryptoMode decrypt() default CryptoMode.GLOBAL;

}
