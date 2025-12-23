package com.xy.lucky.platform.storage;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * MinIO 存储服务封装：
 * - 初始化客户端
 * - 生成预签名上传/下载链接
 * - 按对象键进行流式下载
 */
@Slf4j
@Component
public class MinioStorageService {

    @Autowired
    private MinioProperties properties;

    private MinioClient client;

    @PostConstruct
    public void init() {
        client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * 生成 GET 预签名下载链接
     */
    public String presignedGetUrl(String bucket, String objectKey, int expirySeconds) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .build());
        } catch (Exception e) {
            log.error("presignedGetUrl error bucket={} objectKey={}", bucket, objectKey, e);
            return null;
        }
    }

    /**
     * 生成 PUT 预签名上传链接（支持客户端直传）
     */
    public String presignedPutUrl(String bucket, String objectKey, int expirySeconds) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .build());
        } catch (Exception e) {
            log.error("presignedPutUrl error bucket={} objectKey={}", bucket, objectKey, e);
            return null;
        }
    }

    /**
     * 从 MinIO 流式读取对象并返回响应实体
     */
    public ResponseEntity<Resource> streamObject(String bucket, String objectKey, String fileName, String contentType) {
        try {
            InputStream inputStream = client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            InputStreamResource resource = new InputStreamResource(inputStream);
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            log.error("streamObject error bucket={} objectKey={}", bucket, objectKey, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 上传对象到 MinIO（服务端接收文件后直接写入）
     */
    public void uploadObject(String bucket, String objectKey, InputStream stream, long size, String contentType) {
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(stream, size, -1);
            if (contentType != null && !contentType.isEmpty()) {
                builder.contentType(contentType);
            }
            client.putObject(builder.build());
            log.info("uploadObject: uploaded bucket={} objectKey={} size={}", bucket, objectKey, size);
        } catch (Exception e) {
            log.error("uploadObject error bucket={} objectKey={} size={}", bucket, objectKey, size, e);
            throw new RuntimeException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

}
