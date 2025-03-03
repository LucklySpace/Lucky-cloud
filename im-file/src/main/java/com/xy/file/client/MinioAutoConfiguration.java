package com.xy.file.client;

import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;


@EnableConfigurationProperties(MinioProperties.class)
@Configuration
public class MinioAutoConfiguration {

    @Resource
    private MinioProperties minioProperties;

    /********************************minio相关配置*********************************/
    @Bean
    @ConditionalOnMissingBean({MinioAsyncClient.class})
    public MinioAsyncClient minioClient() {
        Dispatcher dispatcher = new Dispatcher();
        // 设置最大请求数量
        dispatcher.setMaxRequests(minioProperties.getHttpMaxRequest());
        // 设置每台主机的最大请求数量
        dispatcher.setMaxRequestsPerHost(minioProperties.getHttpMaxRequestsPerHost());
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // 取消https验证
                //.sslSocketFactory(SSLUtils.getSSLSocketFactory(), SSLUtils.getX509TrustManager())
                //.hostnameVerifier(SSLUtils.getHostnameVerifier())

                .connectTimeout(minioProperties.getConnectTimeout(), TimeUnit.SECONDS)
                .writeTimeout(minioProperties.getWriteTimeout(), TimeUnit.SECONDS)
                .readTimeout(minioProperties.getReadTimeout(), TimeUnit.SECONDS)
                // 支持协议
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                // 最大空闲连接，保持活动时间
                .connectionPool(new ConnectionPool(minioProperties.getMaxIdleConnections(), minioProperties.getKeepAliveDuration(), TimeUnit.MINUTES))
                .dispatcher(dispatcher).build();

        return MinioAsyncClient.builder().endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .httpClient(okHttpClient)
                .build();
    }


    /**
     * 用于分片上传
     */
    @Bean("pearlMinioClient")
    @ConditionalOnBean({MinioAsyncClient.class})
    @ConditionalOnMissingBean(PearlMinioClient.class)
    public PearlMinioClient customMinioClient(MinioAsyncClient minioClient) {
        return new PearlMinioClient(minioClient);
    }


}

