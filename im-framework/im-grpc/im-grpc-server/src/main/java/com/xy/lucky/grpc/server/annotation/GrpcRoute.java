package com.xy.lucky.grpc.server.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcRoute {
    String value();
}
