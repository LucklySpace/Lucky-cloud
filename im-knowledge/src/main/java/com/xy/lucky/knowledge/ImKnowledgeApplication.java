package com.xy.lucky.knowledge;

import org.dromara.easyes.starter.register.EsMapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableR2dbcRepositories
@EsMapperScan("com.xy.lucky.knowledge.es.mapper")
public class ImKnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImKnowledgeApplication.class, args);
    }
}
