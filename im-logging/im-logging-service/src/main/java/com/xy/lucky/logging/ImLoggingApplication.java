package com.xy.lucky.logging;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@ComponentScan("com.xy.lucky")
@EnableScheduling
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@OpenAPIDefinition(
        info = @Info(
                title = "IM Logging API",
                version = "v1",
                description = "日志采集、管理与分析服务"
        )
)
public class ImLoggingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImLoggingApplication.class, args);
    }
}
