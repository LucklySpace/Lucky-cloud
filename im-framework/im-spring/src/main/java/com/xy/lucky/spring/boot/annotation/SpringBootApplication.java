package com.xy.lucky.spring.boot.annotation;

import com.xy.lucky.spring.annotations.core.ComponentScan;
import com.xy.lucky.spring.annotations.core.Configuration;

import java.lang.annotation.*;

/**
 * SpringBootApplication - Spring Boot 风格的组合注解
 * <p>
 * 相当于同时使用：
 * <ul>
 *   <li>@Configuration - 标识为配置类</li>
 *   <li>@ComponentScan - 启用组件扫描</li>
 * </ul>
 * <p>
 * 用法示例：
 * <pre>
 * &#64;SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Configuration
@ComponentScan
public @interface SpringBootApplication {

    /**
     * 扫描的基础包路径
     * 默认为注解所在类的包路径
     */
    String scanBasePackages() default "";

    /**
     * 排除的自动配置类
     */
    Class<?>[] exclude() default {};

    /**
     * 排除的自动配置类名称
     */
    String[] excludeName() default {};
}
