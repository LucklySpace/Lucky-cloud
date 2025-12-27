package com.xy.lucky.crypto.core.sign.annotation;

import com.xy.lucky.crypto.core.sign.domain.SignatureMode;

import java.lang.annotation.*;


@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Signature {

    // 是否验签
    boolean verify() default true;

    // 是否加签
    boolean sign() default false;

    // 算法
    SignatureMode mode() default SignatureMode.HMAC_SHA256;
}
