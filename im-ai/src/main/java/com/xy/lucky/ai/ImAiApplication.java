package com.xy.lucky.ai;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableCaching
@EnableKnife4j
@OpenAPIDefinition(
        info = @Info(
                title = "IM AI API",
                version = "v1",
                description = "智能对话与知识库接口"
        )
)
public class ImAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImAiApplication.class, args);
    }

}
