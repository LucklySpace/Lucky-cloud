package com.xy.generator;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * ID生成服务启动类
 * 提供多种ID生成策略：Snowflake、Redis Segment、UUID等
 */
@EnableDiscoveryClient
@EnableDubbo(scanBasePackages = "com.xy.generator.service")
@SpringBootApplication
@EnableTransactionManagement
public class ImGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImGeneratorApplication.class, args);
    }

}