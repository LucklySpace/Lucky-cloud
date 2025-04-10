package com.xy.auth;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;



@EnableAsync
@EnableKnife4j
@SpringBootApplication
@EnableConfigurationProperties(value = {SecurityProperties.class, RSAKeyProperties.class})
public class ImAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImAuthApplication.class, args);
    }

}
