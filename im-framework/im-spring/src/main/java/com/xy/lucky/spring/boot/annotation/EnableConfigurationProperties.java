package com.xy.lucky.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * EnableConfigurationProperties - 启用配置属性绑定
 * <p>
 * 用于启用 @ConfigurationProperties 标注类的自动绑定，
 * 也可以指定需要注册为 Bean 的配置类。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableConfigurationProperties({NettyProperties.class, RedisProperties.class})
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
public @interface EnableConfigurationProperties {

    /**
     * 需要注册为 Bean 的配置属性类
     * 如果配置类已经使用 @Component 标注，则无需在此指定
     */
    Class<?>[] value() default {};
}

