package com.xy.spring.annotations.core;

import com.xy.spring.core.ProxyType;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    /**
     * 对组件命名
     *
     * @return 存进容器时的名称
     */
    String value() default "";

    /**
     * 代理模式：NONE = 不代理，JDK = JDK 动态代理，BYTEBUDDY = ByteBuddy 代理，AUTO = 自动选择
     */
    ProxyType proxy() default ProxyType.NONE;

}