package com.xy.lucky.crypto.core.crypto.annotation;

import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;

import java.lang.annotation.*;

/**
 * 字段级别加解密注解
 * <p>
 * 用于标记需要单独加解密的字段，适用于敏感字段如手机号、身份证号等
 * <p>
 * 使用示例：
 * <pre>
 * public class UserDTO {
 *     private String name;
 *
 *     &#64;CryptoField(encrypt = true)
 *     private String phone;       // 手机号加密存储/传输
 *
 *     &#64;CryptoField(encrypt = true, mode = CryptoMode.SM4)
 *     private String idCard;      // 身份证号使用国密加密
 *
 *     &#64;CryptoField(decrypt = true)
 *     private String encryptedData;  // 需要解密的数据
 * }
 * </pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptoField {

    /**
     * 是否在序列化时加密（响应输出）
     */
    boolean encrypt() default false;

    /**
     * 是否在反序列化时解密（请求输入）
     */
    boolean decrypt() default false;

    /**
     * 加解密模式
     */
    CryptoMode mode() default CryptoMode.GLOBAL;

    /**
     * 失败时是否抛出异常
     */
    boolean throwOnFailure() default false;
}

