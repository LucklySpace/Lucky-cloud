package com.xy.lucky.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class}) //去除不必要的组件
public class ImAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImAiApplication.class, args);
    }

}
