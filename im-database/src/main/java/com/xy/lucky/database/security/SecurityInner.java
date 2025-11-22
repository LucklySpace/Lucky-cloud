package com.xy.lucky.database.security;

import java.lang.annotation.*;

/**
 * 内部调用安全注解
 * 用于标记只允许内部服务调用的接口
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SecurityInner {
    /**
     * 是否需要进行AOP统一处理
     * true: 仅允许Feign等内部服务调用
     * false: 允许外部调用
     *
     * @return 是否需要内部调用验证
     */
    boolean value() default true;

    /**
     * 需要特殊判空的字段(预留)
     *
     * @return 字段数组
     */
    String[] field() default {};
}