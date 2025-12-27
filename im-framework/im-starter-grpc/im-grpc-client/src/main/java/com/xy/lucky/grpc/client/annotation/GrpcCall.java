package com.xy.lucky.grpc.client.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcCall {
    // 必填：grpc 请求地址
    String value();

    long timeoutMs() default 0L;

    // 可选：覆盖 grpc 地址，格式 host:port 或 static://host:port
    String address() default "";
}