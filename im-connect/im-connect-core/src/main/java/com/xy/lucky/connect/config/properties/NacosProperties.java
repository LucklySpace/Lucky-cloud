package com.xy.lucky.connect.config.properties;

import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.boot.annotation.ConfigurationProperties;
import com.xy.lucky.spring.boot.annotation.NestedConfigurationProperty;
import lombok.Data;

/**
 * Nacos 配置属性类
 * <p>
 * 对应 application.yml 中的 nacos 配置：
 * <pre>
 * nacos:
 *   enable: true
 *   config:
 *     name: im-connect
 *     address: 127.0.0.1
 *     port: 8848
 *     group: DEFAULT_GROUP
 *     username: nacos
 *     password: nacos
 *     version: 1.0.0
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "nacos")
public class NacosProperties {

    /**
     * 是否启用 Nacos
     */
    private boolean enable = true;

    /**
     * Nacos 配置详情
     */
    @NestedConfigurationProperty
    private NacosConfig config = new NacosConfig();

    /**
     * Nacos 配置详情
     */
    @Data
    public static class NacosConfig {
        /**
         * 服务名称
         */
        private String name = "im-connect";

        /**
         * Nacos 服务器地址
         */
        private String address = "127.0.0.1";

        /**
         * Nacos 服务器端口
         */
        private int port = 8848;

        /**
         * 分组
         */
        private String group = "DEFAULT_GROUP";

        /**
         * 用户名
         */
        private String username = "nacos";

        /**
         * 密码
         */
        private String password = "nacos";

        /**
         * 版本号
         */
        private String version = "1.0.0";

        /**
         * 命名空间
         */
        private String namespace = "";
    }
}

