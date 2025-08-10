package com.xy.update;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.xy.update.config.TauriUpdaterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableKnife4j
@EnableAsync
@ComponentScan("com.xy") // 扫描包路径
@EnableFeignClients(basePackages = "com.xy.update") //开启openfeign
@EnableConfigurationProperties(TauriUpdaterProperties.class)
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class}) //去除不必要的组件
public class ImUpdateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImUpdateApplication.class, args);
    }

}
