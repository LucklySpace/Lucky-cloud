package com.xy.lucky.quartz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class ImQuartzApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImQuartzApplication.class, args);
    }
}
