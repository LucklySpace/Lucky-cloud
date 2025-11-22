package com.xy.lucky.file.client;

import io.minio.MinioAsyncClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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


@Slf4j
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
        dispatcher.setMaxRequests(minioProperties.getHttpMaxRequest());
        dispatcher.setMaxRequestsPerHost(minioProperties.getHttpMaxRequestsPerHost());

        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder()
                .connectTimeout(minioProperties.getConnectTimeout(), TimeUnit.SECONDS)
                .writeTimeout(minioProperties.getWriteTimeout(), TimeUnit.SECONDS)
                .readTimeout(minioProperties.getReadTimeout(), TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .connectionPool(new ConnectionPool(minioProperties.getMaxIdleConnections(),
                        minioProperties.getKeepAliveDuration(), TimeUnit.MINUTES))
                .dispatcher(dispatcher)
                .addInterceptor(chain -> {
                    var request = chain.request();
                    var newRequest = request.newBuilder()
                            .header("User-Agent", "MinIO (Java; amd64) minio-java/8.5.7")
                            .header("Accept", "*/*")
                            .build();
                    return chain.proceed(newRequest);
                });

        // 可选：在调试时加入 OkHttp 日志拦截器（部署时请移除或降低级别，避免泄露敏感 header）
        // HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> log.debug("OKHTTP: {}", message));
        // logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        // httpBuilder.addInterceptor(logging);

        OkHttpClient okHttpClient = httpBuilder.build();

        String endpoint = minioProperties.getEndpoint() == null ? null : minioProperties.getEndpoint().trim();
        String ak = minioProperties.getAccessKey() == null ? null : minioProperties.getAccessKey().trim();
        String sk = minioProperties.getSecretKey() == null ? null : minioProperties.getSecretKey().trim();

        MinioAsyncClient client = MinioAsyncClient.builder()
                    .endpoint(endpoint)
                    .credentials(ak, sk)
                    .httpClient(okHttpClient)
                    .build();

        // 强制 path-style（如果你的 MinIO 没启用 MINIO_DOMAIN，可以禁用 virtual-style）
        // 这通常能解决多数“签名计算不一致”的问题（Host/path 不一致）
        try {
            client.disableVirtualStyleEndpoint();
            log.info("MinIO client: virtual-style endpoint disabled (using path-style requests).");
        } catch (Exception ex) {
            log.warn("Disable virtual-style failed or not supported: {}", ex.getMessage());
        }

        return client;
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