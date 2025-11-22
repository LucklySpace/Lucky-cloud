package com.xy.crypto.sign.annotation;

import com.xy.crypto.sign.domain.SignatureMode;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Signature {

    // 是否验签
    boolean verify() default true;

    // 是否加签
    boolean sign() default false;

    // 算法
    SignatureMode mode() default SignatureMode.HMAC_SHA256;
}
