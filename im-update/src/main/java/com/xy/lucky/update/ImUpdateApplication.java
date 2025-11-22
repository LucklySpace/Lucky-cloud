package com.xy.lucky.update;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.xy.lucky.update.config.TauriUpdaterProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 应用更新服务启动类
 */
@EnableKnife4j
@EnableAsync
@ComponentScan("com.xy") // 扫描包路径
@EnableFeignClients("com.xy.lucky.update") // 开启 OpenFeign
@EnableConfigurationProperties(TauriUpdaterProperties.class)
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class})
@OpenAPIDefinition(
        info = @Info(
                title = "IM Update API",
                version = "v1",
                description = "应用更新服务 API（Tauri 自动更新相关接口）"
        )
)
public class ImUpdateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImUpdateApplication.class, args);
    }
}
