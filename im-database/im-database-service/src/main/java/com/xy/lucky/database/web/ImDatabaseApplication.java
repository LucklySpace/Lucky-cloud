package com.xy.lucky.database.web;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableKnife4j
@EnableAsync
@EnableDubbo(scanBasePackages = "com.xy.lucky.database.web")
@EnableTransactionManagement  //开启事务管理
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class ImDatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImDatabaseApplication.class, args);
    }

}
