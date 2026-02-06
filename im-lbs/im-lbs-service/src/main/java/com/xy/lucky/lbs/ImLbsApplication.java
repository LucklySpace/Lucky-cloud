package com.xy.lucky.lbs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
@ComponentScan("com.xy.lucky")
@OpenAPIDefinition(
        info = @Info(
                title = "IM Lbs API",
                version = "v1",
                description = "地理位置基础服务  用于定位, 附近的人，位置上报等等"
        )
)
public class ImLbsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImLbsApplication.class, args);
    }
}
