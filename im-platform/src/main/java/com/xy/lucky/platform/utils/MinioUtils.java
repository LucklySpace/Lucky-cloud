package com.xy.lucky.platform.utils;

import com.xy.lucky.platform.config.MinioProperties;
import com.xy.lucky.platform.exception.FileException;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.string.StringUtils;
import io.minio.*;
import io.minio.errors.MinioException;
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

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO 存储服务封装：
 * - 初始化客户端
 * - 生成预签名上传/下载链接
 * - 按对象键进行流式下载
 */
@Slf4j
@Component
public class MinioUtils {

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
     * 公开文件路径（不签名）
     */
    public String presignedGetUrl(String bucket, String objectName) {
        return StringUtils.format("{}/{}/{}", properties.getEndpoint(), bucket, objectName);
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

    /**
     * 设置bucket权限为public
     *
     * @return Boolean
     */
    public void setBucketPublic(String bucket) {

        try {

            // 检查Bucket是否存在
            boolean isExist = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!isExist) {
                System.out.println("Bucket " + bucket + " does not exist");
                return;
            }

            // 设置公开
            String policyJson =
                    "{\"Version\":\"2012-10-17\"," + "\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":"
                            + "{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\","
                            + "\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::" + bucket
                            + "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:PutObject\",\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListMultipartUploadParts\"],\"Resource\":[\"arn:aws:s3:::"
                            + bucket + "/*\"]}]}";

            // 设置Bucket策略
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(policyJson)
                    .build());

            log.info("Successfully set public access policy for " + bucket);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error occurred: " + e);
            throw new FileException("设置文件桶公开失败");
        }
    }

    /**
     * 判断对象是否存在
     */
    public boolean isExist(String bucket, String objectKey) {
        try {
            return client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
                    && client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build()) != null;
        } catch (Exception e) {
            log.error("isExist error bucket={} objectKey={}", bucket, objectKey, e);
            return false;
        }
    }

    /**
     * 生成新对象名称 （使用uuid生成）
     */
    public String getObjectName(String fileName) {
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        return IdUtils.base62Uuid() + prefix;
    }

    /**
     * 删除对象
     */
    public boolean removeObject(String bucket, String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("removeObject: removed bucket={} objectKey={}", bucket, objectKey);
            return true;
        } catch (Exception e) {
            log.error("removeObject error bucket={} objectKey={}", bucket, objectKey, e);
            return false;
        }
    }

}
