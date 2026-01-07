package com.xy.lucky.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * NestedConfigurationProperty - 标记嵌套配置属性
 * <p>
 * 用于标记配置类中的嵌套对象属性，以便正确绑定嵌套的配置结构。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;ConfigurationProperties(prefix = "netty")
 * public class NettyProperties {
 *
 *     &#64;NestedConfigurationProperty
 *     private WebSocketConfig websocket;
 *
 *     public static class WebSocketConfig {
 *         private boolean enable;
 *         private List&lt;Integer&gt; port;
 *     }
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NestedConfigurationProperty {
}

