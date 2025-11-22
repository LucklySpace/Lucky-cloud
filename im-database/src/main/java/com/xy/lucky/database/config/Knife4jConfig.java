package com.xy.lucky.database.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("im-database")
                        .description("im-server接口文档")
                        .version("v1.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}