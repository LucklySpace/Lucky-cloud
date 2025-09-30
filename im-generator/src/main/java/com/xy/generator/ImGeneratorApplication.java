package com.xy.generator;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@ComponentScan(basePackages = {"com.xy.generator", "com.xy.grpc"})
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement  //开启事务管理
public class ImGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImGeneratorApplication.class, args);
    }

}


