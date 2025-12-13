package com.xy.lucky.leaf;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * ID生成服务启动类
 * 提供多种ID生成策略：Snowflake、Redis Segment、UUID等
 */
@EnableDiscoveryClient
@EnableDubbo(scanBasePackages = "com.xy.lucky.leaf.service")
@EnableTransactionManagement
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class}) //去除不必要的组件
public class ImLeafApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImLeafApplication.class, args);
    }

}