package com.xy.lucky.grpc.client.annotation;


import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcClient {

    String name() default "";

    String value() default "generic";
}