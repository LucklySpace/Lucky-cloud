package com.xy.general.version.annotations;


import java.lang.annotation.*;

/**
 * 版本
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
    //版本号的值，从1开始
    String value() default "1";
}