package com.xy.lucky.spring.annotations.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
public @interface Autowired {
    /**
     * Bean 名称（类名或自定义名称）。
     * - 为空时：按字段类型注入。
     * - 非空时：按名称注入（支持类全名或简名）。
     *
     * @return Bean 名称，默认空字符串
     */
    String name() default "";

    /**
     * 别名：value() 等同于 name()，Spring 兼容。
     *
     * @return Bean 名称，默认空字符串
     */
    String value() default "";

    /**
     * 是否必填注入。
     *
     * @return 是否 required，默认 true
     */
    boolean required() default true;
}
