package com.xy.lucky.oss;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@EnableKnife4j
@EnableAsync
@EnableDubbo(scanBasePackages = "com.xy.lucky.oss")
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class ImOssApplication {

    public static void main(String[] args) {
        System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
        System.setProperty("aws.java.v1.printLocation", "false");
        SpringApplication.run(ImOssApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
