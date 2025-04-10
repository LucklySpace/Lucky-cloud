package com.xy.server;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableKnife4j
@EnableAsync
@EnableTransactionManagement  //开启事务管理
@SpringBootApplication
public class ImServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImServerApplication.class, args);
    }

}
