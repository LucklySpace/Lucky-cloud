package com.xy.lucky.job.annotation;

import java.lang.annotation.*;

/**
 * 分布式任务注解
 * 标记在方法上，表示该方法是一个可被调度的任务
 *
 * @author lucky
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LuckyJob {

    /**
     * 任务名称，全局唯一
     * 如果为空，默认使用方法名
     */
    String value() default "";

    /**
     * 任务描述
     */
    String description() default "";

    /**
     * 初始参数
     */
    String initParams() default "";
}
