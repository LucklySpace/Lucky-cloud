package com.xy.lucky.server.utils;

import com.xy.lucky.utils.time.DateTimeUtils;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;

@Slf4j
@Component
public class MinioUtil {

    @Value(value = "${minio.endpoint}")
    private String endpoint;
    @Value(value = "${minio.accesskey}")
    private String accesskey;
    @Value(value = "${minio.secretkey}")
    private String secretkey;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accesskey, secretkey)
                .build();
    }


    /**
     * 查看存储bucket是否存在
     *
     * @return Mono<Boolean>
     */
    public Mono<Boolean> bucketExists(String bucketName) {
        return Mono.fromCallable(() -> {
            try {
                return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            } catch (Exception e) {
                log.error("查询bucket失败", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 创建存储bucket
     *
     * @return Mono<Boolean>
     */
    public Mono<Boolean> makeBucket(String bucketName) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                return true;
            } catch (Exception e) {
                log.error("创建bucket失败,", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 设置bucket权限为public
     *
     * @return Mono<Boolean>
     */
    public Mono<Boolean> setBucketPublic(String bucketName) {
        return Mono.fromCallable(() -> {
            try {
                // 设置公开
                String sb =
                        "{\"Version\":\"2012-10-17\"," + "\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":"
                                + "{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\","
                                + "\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::" + bucketName
                                + "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:PutObject\",\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListMultipartUploadParts\"],\"Resource\":[\"arn:aws:s3:::"
                                + bucketName + "/*\"]}]}";
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucketName).config(sb).build());
                return true;
            } catch (Exception e) {
                log.error("创建bucket失败,", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除存储bucket
     *
     * @return Mono<Boolean>
     */
    public Mono<Boolean> removeBucket(String bucketName) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
                return true;
            } catch (Exception e) {
                log.error("删除bucket失败,", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 文件上传 (InputStream)
     */
    public Mono<String> upload(String bucketName, String path, String originalFilename, InputStream stream, long size, String contentType) {
        return Mono.fromCallable(() -> {
            if (StringUtils.isBlank(originalFilename)) {
                throw new RuntimeException("文件名不能为空");
            }
            String fileName =
                    System.currentTimeMillis() + originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName =
                    DateTimeUtils.format(LocalDate.now(), DateTimeUtils.PART_DATE_FORMAT_TWO) + "/" + fileName;
            try {
                PutObjectArgs objectArgs = PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path + "/" + objectName)
                        .stream(stream, size, -1)
                        .contentType(contentType)
                        .build();
                // 文件名称相同会覆盖
                minioClient.putObject(objectArgs);
                return objectName;
            } catch (Exception e) {
                log.error("上传文件失败,", e);
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 文件上传
     *
     * @param bucketName  bucket名称
     * @param path        路径
     * @param name        文件名
     * @param fileByte    文件内容
     * @param contentType
     * @return Mono<String>
     */
    public Mono<String> upload(String bucketName, String path, String name, byte[] fileByte,
                         String contentType) {
        return Mono.fromCallable(() -> {
            String fileName = System.currentTimeMillis() + name.substring(name.lastIndexOf("."));
            String objectName =
                    DateTimeUtils.format(LocalDate.now(), DateTimeUtils.PART_DATE_FORMAT_TWO) + "/" + fileName;
            try {
                InputStream stream = new ByteArrayInputStream(fileByte);
                PutObjectArgs objectArgs = PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path + "/" + objectName)
                        .stream(stream, fileByte.length, -1)
                        .contentType(contentType)
                        .build();
                // 文件名称相同会覆盖
                minioClient.putObject(objectArgs);
                return objectName;
            } catch (Exception e) {
                log.error("上传图片失败,", e);
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除
     *
     * @param bucketName bucket名称
     * @param fileName
     * @return Mono<Boolean>
     * @throws Exception
     * @path path
     */
    public Mono<Boolean> remove(String bucketName, String path, String fileName) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder().bucket(bucketName).object(path + fileName).build());
                return true;
            } catch (Exception e) {
                log.error("删除图片失败,", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
