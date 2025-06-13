package com.xy.generator;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@ComponentScan("com.xy") // 扫描包路径
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement  //开启事务管理
public class ImGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImGeneratorApplication.class, args);
    }

}


