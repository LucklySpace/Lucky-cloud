package com.xy.lucky.platform.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
@Schema(description = "MinIO配置信息")
public class MinioProperties {

    @Schema(description = "MinIO服务地址")
    private String endpoint;

    @Schema(description = "MinIO外网访问")
    private String extranet;

    @Schema(description = "MinIO访问用户名")
    private String accessKey;

    @Schema(description = "MinIO密钥")
    private String secretKey;

    @Schema(description = "MinIO存储桶名称")
    private String bucketName;
}

