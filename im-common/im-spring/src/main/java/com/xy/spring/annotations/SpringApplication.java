package com.xy.spring.annotations;

import com.xy.spring.annotations.core.ComponentScan;
import com.xy.spring.annotations.core.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ComponentScan
public @interface SpringApplication {
    String value() default ""; // 支持传入 basePackage
}