package com.xy.lucky.file;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@ComponentScan(basePackages = {"com.xy.lucky.general", "com.xy.lucky.file"})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@OpenAPIDefinition(
        info = @Info(
                title = "IM File Service",
                version = "v1",
                description = "文件上传/下载与媒体处理接口"
        ),
        servers = {
                @Server(url = "/", description = "Default Server")
        }
)
public class ImFileApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImFileApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
