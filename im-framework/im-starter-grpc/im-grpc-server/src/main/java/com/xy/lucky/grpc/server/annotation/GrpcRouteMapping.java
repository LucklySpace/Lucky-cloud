package com.xy.lucky.grpc.server.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcRouteMapping {
    String value() default "";
}