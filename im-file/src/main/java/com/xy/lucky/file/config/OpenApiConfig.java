package com.xy.lucky.file.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi imFileOpenApi() {
        return GroupedOpenApi.builder()
                .group("im-file")
                .packagesToScan("com.xy.lucky.file.controller")
                .pathsToMatch("/api/**")
                .build();
    }
}
