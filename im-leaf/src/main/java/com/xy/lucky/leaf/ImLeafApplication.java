package com.xy.lucky.leaf;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableDiscoveryClient
@EnableDubbo(scanBasePackages = "com.xy.lucky.leaf.service")
@EnableTransactionManagement
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@OpenAPIDefinition(
        info = @Info(
                title = "IM Leaf API",
                version = "v1",
                description = "分布式ID生成服务"
        )
)
public class ImLeafApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImLeafApplication.class, args);
    }
}
