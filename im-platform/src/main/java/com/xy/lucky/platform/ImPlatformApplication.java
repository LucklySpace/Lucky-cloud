package com.xy.lucky.platform;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Tauri 应用更新服务启动类
 *
 * 提供以下主要功能：
 * 1. 版本信息发布和管理
 * 2. 应用更新包上传和存储（基于MinIO）
 * 3. 客户端更新检查和下载服务
 *
 */

@EnableAsync
@ComponentScan("com.xy.lucky")
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@OpenAPIDefinition(
        info = @Info(
                title = "IM Platform API",
                version = "v1",
                description = "基础平台服务  支持应用发布更新以及短链"
        )
)
public class ImPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImPlatformApplication.class, args);
    }
}