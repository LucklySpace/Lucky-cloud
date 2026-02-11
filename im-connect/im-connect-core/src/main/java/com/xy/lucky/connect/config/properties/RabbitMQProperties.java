package com.xy.lucky.connect.config.properties;

import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.boot.annotation.ConfigurationProperties;
import lombok.Data;

/**
 * RabbitMQ 配置属性类
 * <p>
 * 对应 application.yml 中的 rabbitmq 配置：
 * <pre>
 * rabbitmq:
 *   address: 127.0.0.1
 *   port: 5672
 *   username: guest
 *   password: guest
 *   virtual: /
 *   exchange: IM-SERVER
 *   routingKeyPrefix: IM-
 *   errorQueue: im.error
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {

    /**
     * RabbitMQ 地址
     */
    private String address = "127.0.0.1";

    /**
     * RabbitMQ 端口
     */
    private int port = 5672;

    /**
     * 用户名
     */
    private String username = "guest";

    /**
     * 密码
     */
    private String password = "guest";

    /**
     * 虚拟主机
     */
    private String virtual = "/";

    /**
     * 交换机名称
     */
    private String exchange = "IM-SERVER";

    /**
     * 路由键前缀
     */
    private String routingKeyPrefix = "IM-";

    /**
     * 错误队列名称
     */
    private String errorQueue = "im.error";

    /**
     * 连接超时时间
     */
    private int connectionTimeout = 60000;

    /**
     * 是否自动恢复连接
     */
    private boolean automaticRecovery = true;
}

