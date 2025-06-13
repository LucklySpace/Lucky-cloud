package com.xy.database;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableKnife4j
@EnableAsync
@EnableTransactionManagement  //开启事务管理
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class}) //去除不必要的组件
public class ImDatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImDatabaseApplication.class, args);
    }

}
