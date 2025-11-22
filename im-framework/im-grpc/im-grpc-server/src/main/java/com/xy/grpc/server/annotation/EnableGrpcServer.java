package com.xy.grpc.server.annotation;

import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan
public @interface EnableGrpcServer {
    // 要扫描的包，默认空表示从主类所在包开始（在 Registrar 实现里会处理空情况）
    String[] basePackages() default {};
}