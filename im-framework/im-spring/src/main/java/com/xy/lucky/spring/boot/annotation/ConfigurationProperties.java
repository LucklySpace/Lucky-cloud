package com.xy.lucky.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * ConfigurationProperties - 配置属性绑定注解
 * <p>
 * 将 YAML/Properties 配置绑定到 Java Bean 上。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Component
 * &#64;ConfigurationProperties(prefix = "netty.config")
 * public class NettyProperties {
 *     private String protocol;
 *     private int heartBeatTime;
 *     // getters and setters
 * }
 * </pre>
 * <p>
 * 对应的 YAML 配置：
 * <pre>
 * netty:
 *   config:
 *     protocol: proto
 *     heartBeatTime: 30000
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {

    /**
     * 配置前缀
     * 例如：prefix = "netty.config" 将绑定 netty.config.* 下的所有属性
     */
    String prefix() default "";

    /**
     * 配置前缀的别名
     */
    String value() default "";

    /**
     * 是否忽略无效的字段（配置中有但类中没有的字段）
     */
    boolean ignoreUnknownFields() default true;

    /**
     * 是否忽略无效的值（类型转换失败时）
     */
    boolean ignoreInvalidFields() default false;
}

