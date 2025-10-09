package com.xy.file.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * minio配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * OSS 访问端点，集群时需提供统一入口
     */
    private String endpoint;

    /**
     * 访问方式，http/https
     */
    private String extranet;

    /**
     * 用户名
     */
    private String accessKey;

    /**
     * 密码
     */
    private String secretKey;

    /**
     * 存储桶名
     */
    private String bucketName;


    /**
     * 链接有效时间，单位 秒，默认 7天
     */
    private Integer linkExpiry = 7 * 24 * 60 * 60;

    /**
     * 是否创建缩略图
     */
    private Boolean createThumbnail = Boolean.FALSE;

    /**
     * 是否创建水印
     */
    private Boolean createWatermark = Boolean.FALSE;

    /**
     * 是否压缩
     */
    private Boolean isCompress = Boolean.FALSE;

    /**
     * 是否检查
     */
    private Boolean isChecked = Boolean.FALSE;

    /**
     * 是否计算音视频时长
     */
    private Boolean calculationDuration = Boolean.TRUE;

    /**
     * 总的最大链接数
     */
    private Integer httpMaxRequest = 128;

    /**
     * 每台机器的最大请求数量
     */
    private Integer httpMaxRequestsPerHost = 10;

    /**
     * 链接超时时间，单位 秒，默认 1 分钟
     */
    private Integer connectTimeout = 60;

    /**
     * 写超时时间，单位 秒，默认 1 分钟
     */
    private Integer writeTimeout = 60;

    /**
     * 读取超时时间，单位 秒，默认 1 分钟
     */
    private Integer readTimeout = 60;

    /**
     * http链接最大空闲数
     */
    private Integer maxIdleConnections = 10;

    /**
     * 保持keepAlive的时间，单位 分钟
     */
    private Long keepAliveDuration = 5L;

}