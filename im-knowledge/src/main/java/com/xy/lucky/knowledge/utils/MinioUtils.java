package com.xy.lucky.knowledge.utils;

import com.xy.lucky.knowledge.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public Mono<String> upload(String objectName, InputStream inputStream, long size, String contentType) {
        return Mono.fromCallable(() -> {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return objectName;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> remove(String objectName) {
        return Mono.fromRunnable(() -> {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectName)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("删除对象失败", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<String> getPresignedUrl(String objectName) {
        return Mono.fromCallable(() ->
                minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioProperties.getBucket())
                        .object(objectName)
                        .expiry(60 * 60) // 1 小时
                        .build())
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<byte[]> getObjectBytes(String objectName) {
        return Mono.fromCallable(() -> {
            try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .build())) {
                return is.readAllBytes();
            } catch (Exception e) {
                throw new RuntimeException("读取对象失败", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

//    public Mono<InputStream> getObject(String objectName) {
//         return Mono.fromCallable(() ->
//             minioClient.getObject(io.minio.GetObjectArgs.builder()
//                     .bucket(minioProperties.getBucket())
//                     .object(objectName)
//                     .build())
//         ).subscribeOn(Schedulers.boundedElastic());
//    }
}
