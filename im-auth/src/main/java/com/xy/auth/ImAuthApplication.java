package com.xy.auth;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.xy.auth.security.IMRSAKeyProperties;
import com.xy.auth.security.IMSecurityProperties;
import com.xy.crypto.CryptoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@EnableKnife4j
@EnableAspectJAutoProxy(exposeProxy = true)
@ComponentScan("com.xy") // 扫描包路径
@EnableFeignClients(basePackages = "com.xy.auth.api") //开启openfeign
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class}) //去除不必要的组件
@EnableConfigurationProperties(value = {IMSecurityProperties.class, IMRSAKeyProperties.class, CryptoProperties.class})
public class ImAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImAuthApplication.class, args);
    }

}
