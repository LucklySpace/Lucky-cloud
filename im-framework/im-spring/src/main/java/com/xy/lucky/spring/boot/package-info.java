/**
 * Lucky Spring Boot 核心包
 * <p>
 * 提供类似 Spring Boot 的启动器功能：
 * <ul>
 *   <li>{@link com.xy.lucky.spring.boot.SpringApplication} - 应用启动器</li>
 *   <li>{@link com.xy.lucky.spring.boot.annotation.SpringBootApplication} - 组合注解</li>
 *   <li>{@link com.xy.lucky.spring.boot.context.ConfigurableApplicationContext} - 可配置上下文</li>
 *   <li>{@link com.xy.lucky.spring.boot.env.Environment} - 环境抽象</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * &#64;SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 */
package com.xy.lucky.spring.boot;

