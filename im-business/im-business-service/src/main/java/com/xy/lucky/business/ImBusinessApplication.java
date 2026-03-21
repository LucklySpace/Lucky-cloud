package com.xy.lucky.business;

import com.xy.lucky.business.config.ServerRuntimeHintsConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableDubbo
@ComponentScan("com.xy.lucky")
@EnableTransactionManagement
@SpringBootApplication(exclude = {ManagementWebSecurityAutoConfiguration.class, DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class})
@ImportRuntimeHints(ServerRuntimeHintsConfig.class)
public class ImBusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImBusinessApplication.class, args);
    }
}
