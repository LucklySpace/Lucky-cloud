package com.xy.lucky.auth;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.xy.lucky.auth.security.config.OAuth2Properties;
import com.xy.lucky.auth.security.config.RSAKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableAsync
@EnableScheduling
@EnableKnife4j
@EnableAspectJAutoProxy(exposeProxy = true)
@ComponentScan("com.xy.lucky") // 扫描包路径
@EnableConfigurationProperties({RSAKeyProperties.class, OAuth2Properties.class})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) //去除不必要的组件
public class ImAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImAuthApplication.class, args);
    }

}
