package com.xy.lucky.connect.config.properties;

import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.boot.annotation.ConfigurationProperties;
import lombok.Data;

/**
 * Redis 配置属性类
 * <p>
 * 对应 application.yml 中的 redis 配置：
 * <pre>
 * redis:
 *   host: 127.0.0.1
 *   port: 6379
 *   password: xxx
 *   timeout: 10000
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {

    /**
     * Redis 主机地址
     */
    private String host = "127.0.0.1";

    /**
     * Redis 端口
     */
    private int port = 6379;

    /**
     * Redis 密码
     */
    private String password;

    /**
     * 连接超时时间（毫秒）
     */
    private int timeout = 10000;

    /**
     * 数据库索引
     */
    private int database = 0;

    /**
     * 连接池最大连接数
     */
    private int maxTotal = 8;

    /**
     * 连接池最大空闲连接数
     */
    private int maxIdle = 8;

    /**
     * 连接池最小空闲连接数
     */
    private int minIdle = 0;
}

