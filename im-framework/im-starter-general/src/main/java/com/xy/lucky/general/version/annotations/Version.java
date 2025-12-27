package com.xy.lucky.general.version.annotations;

import java.lang.annotation.*;

/**
 * API版本控制注解
 * <p>
 * 用于标记控制器类或方法的API版本，支持URI路径版本控制
 * 示例: @Version("1") 标记为v1版本的API
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
    /**
     * 版本号
     *
     * @return 版本号字符串，默认为"1"
     */
    String value() default "1";
}