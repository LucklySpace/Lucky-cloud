package com.xy.lucky.quartz.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lucky Cloud 任务调度中心 API")
                        .version("1.0")
                        .description("基于 Quartz 的分布式任务调度系统接口文档")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
