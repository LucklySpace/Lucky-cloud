package com.xy.lucky.spring.annotations.core;

import java.lang.annotation.*;

/**
 * Component - 组件注解
 * <p>
 * 标记一个类为 Spring 管理的组件，会被自动扫描并注册到容器中
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    /**
     * Bean 名称
     * <p>
     * 如果不指定，默认使用类名首字母小写
     *
     * @return Bean 名称
     */
    String value() default "";
}
