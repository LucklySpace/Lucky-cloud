package com.xy.auth;

import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties(value = {SecurityProperties.class, RSAKeyProperties.class})
public class ImAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImAuthApplication.class, args);
    }

}
