package com.xy.lucky.oss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;


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
