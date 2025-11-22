package com.xy.lucky.grpc.client.annotation;


import com.xy.lucky.grpc.client.GrpcClientRegistrar;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ComponentScan
@Import(GrpcClientRegistrar.class)
public @interface EnableGrpcClient {
    // 要扫描的包，默认空表示从主类所在包开始（在 Registrar 实现里会处理空情况）
    String[] basePackages() default {};
}
