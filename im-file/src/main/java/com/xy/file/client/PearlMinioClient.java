package com.xy.file.client;

import com.google.common.collect.Multimap;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class PearlMinioClient extends MinioAsyncClient {


    protected PearlMinioClient(MinioAsyncClient client) {
        super(client);
    }

    /**
     * 创建分片上传请求
     *
     * @param bucketName       存储桶
     * @param region           区域
     * @param objectName       对象名
     * @param headers          消息头
     * @param extraQueryParams 额外查询参数
     */
    @Override
    public CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadAsync(String bucketName, String region, String objectName, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws InsufficientDataException, InternalException, InvalidKeyException, IOException, NoSuchAlgorithmException, XmlParserException {
        return super.createMultipartUploadAsync(bucketName, region, objectName, headers, extraQueryParams);
    }

    /**
     * 完成分片上传，执行合并文件
     *
     * @param bucketName       存储桶
     * @param region           区域
     * @param objectName       对象名
     * @param uploadId         上传ID
     * @param parts            分片
     * @param extraHeaders     额外消息头
     * @param extraQueryParams 额外查询参数
     */
    @Override
    public CompletableFuture<ObjectWriteResponse> completeMultipartUploadAsync(String bucketName, String region, String objectName, String uploadId, Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, XmlParserException, InternalException {
        return super.completeMultipartUploadAsync(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams);
    }

    /**
     * 查询分片数据
     *
     * @param bucketName       存储桶
     * @param region           区域
     * @param objectName       对象名
     * @param uploadId         上传ID
     * @param extraHeaders     额外消息头
     * @param extraQueryParams 额外查询参数
     */
    public CompletableFuture<ListPartsResponse> listMultipart(String bucketName, String region, String objectName, Integer maxParts, Integer partNumberMarker, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, ServerException, XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        return super.listPartsAsync(bucketName, region, objectName, maxParts, partNumberMarker, uploadId, extraHeaders, extraQueryParams);
    }


    /**
     * 中止分块上传任务
     *
     * @param bucket           存储桶
     * @param region           区域
     * @param objectName       对象名
     * @param uploadId         上传ID
     * @param extraHeaders     额外消息头
     * @param extraQueryParams 额外查询参数
     */
    public String removeMultipartUpload(String bucket, String region, String objectName, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException, ExecutionException, InterruptedException {
        CompletableFuture<AbortMultipartUploadResponse> response = this.abortMultipartUploadAsync(bucket, region, objectName, uploadId, extraHeaders, extraQueryParams);
        return response.get().uploadId();
    }

    /**
     * 批量清理过期上传任务
     *
     * @param bucket         存储桶
     * @param region         区域
     * @param expirationDays 过期天数
     */
    public void clearMultipartUpload(String bucket,
                                     String region,
                                     int expirationDays) {
        try {
            CompletableFuture<ListMultipartUploadsResponse> multiUploads =
                    this.listMultipartUploadsAsync(bucket, region, null, null, null, 1000, null, null, null, null);

            multiUploads.thenAccept(response -> {
                if (response != null && response.result() != null) {
                    response.result().uploads().forEach(upload -> {
                        try {
                            if (isExpired(upload.initiated(), expirationDays)) {
                                removeMultipartUpload(bucket, region, upload.objectName(),
                                        upload.uploadId(), null, null);
                                log.info("Removed expired upload: {}", upload.objectName());
                            }
                        } catch (Exception e) {
                            log.error("Failed to remove multipart upload: {}", e.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            log.error("Clear multipart upload failed: {}", e.getMessage());
        }
    }

    private boolean isExpired(ZonedDateTime uploadTime, int expirationDays) {
        return uploadTime.plusDays(expirationDays).isBefore(ZonedDateTime.now());
    }

    /**
     * 获取文件元信息
     *
     * @param bucketName 桶名称
     * @param objectName 文件名称
     * @return ObjectStat 文件元信息
     */
    public CompletableFuture<StatObjectResponse> getObjectMetadata(String bucketName, String objectName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException, ExecutionException, InterruptedException {
        return this.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 下载文件
     *
     * @param bucketName 桶名称
     * @param objectName 文件名称
     * @return InputStream 文件流
     */
    public CompletableFuture<GetObjectResponse> downloadFile(String bucketName, String objectName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException, ExecutionException, InterruptedException {
        return this.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 取消分片上传
     *
     * @param bucketName       String   桶名称
     * @param region           String
     * @param objectName       String   文件名称
     * @param uploadId         String   上传的 uploadId
     * @param extraHeaders     Multimap<String, String>
     * @param extraQueryParams Multimap<String, String>
     * @return
     */
    public AbortMultipartUploadResponse abortMultipartPart(String bucketName, String region, String objectName,
                                                           String uploadId, Multimap<String, String> extraHeaders,
                                                           Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            ServerException, XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        return this.abortMultipartUpload(bucketName, region, objectName, uploadId, extraHeaders, extraQueryParams);
    }


    /**
     * 上传文件
     *
     * @param bucketName  存储桶
     * @param objectName  对象名
     * @param contentType 内容类型
     * @param inputStream 输入流
     * @param partSize    分片大小
     * @param metadata    元数据
     */
    public CompletableFuture<ObjectWriteResponse> uploadFile(String bucketName,
                                                             String objectName,
                                                             String contentType,
                                                             InputStream inputStream,
                                                             long partSize,
                                                             Map<String, String> metadata) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, -1, partSize)
                    .contentType(contentType);

            if (metadata != null && !metadata.isEmpty()) {
                builder.userMetadata(metadata);
            }

            return this.putObject(builder.build());
        } catch (Exception e) {
            log.error("Upload file failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 上传文件
     *
     * @param bucketName  存储桶
     * @param objectName  对象名
     * @param contentType 内容类型
     * @param inputStream 输入流
     */
    public void uploadFile(String bucketName, String objectName, String contentType, InputStream inputStream) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        this.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(inputStream, -1, 10485760)
                .contentType(contentType)
                .build());
    }


    /**
     * 删除文件
     *
     * @param bucketName 桶名称
     * @param objectName 文件名称
     */
    public void deleteFile(String bucketName, String objectName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        this.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 检查桶是否存在
     *
     * @param bucketName 桶名称
     * @return boolean 是否存在
     */
    public CompletableFuture<Boolean> doesBucketExist(String bucketName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException, ExecutionException, InterruptedException {
        return this.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
    }

    /**
     * 删除桶
     *
     * @param bucketName 桶名称
     */
    public void deleteBucket(String bucketName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        this.removeBucket(RemoveBucketArgs.builder()
                .bucket(bucketName)
                .build());
    }

    /**
     * 列出桶中的所有文件
     *
     * @param bucketName 桶名称
     * @return List<String> 文件名称列表
     */
    public List<String> listFiles(String bucketName) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        List<String> fileNames = new ArrayList<>();
        Iterable<Result<Item>> results = this.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .build());
        for (Result<Item> result : results) {
            fileNames.add(result.get().objectName());
        }
        return fileNames;
    }

    /**
     * 生成文件访问的预签名 URL
     *
     * @param bucketName 桶名称
     * @param objectName 文件名称
     * @param expiry     URL 有效时间（秒）
     * @return String 预签名 URL
     */
    public String generateFileUrl(String bucketName, String objectName, int expiry) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        return this.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .expiry(expiry, TimeUnit.SECONDS)
                .method(io.minio.http.Method.GET)
                .build());
    }

    /**
     * 复制文件
     *
     * @param sourceBucket 源存储桶
     * @param sourceObject 源对象
     * @param targetBucket 目标存储桶
     * @param targetObject 目标对象
     * @param copyMetadata 是否复制元数据
     */
    public CompletableFuture<ObjectWriteResponse> copyFile(String sourceBucket,
                                                           String sourceObject,
                                                           String targetBucket,
                                                           String targetObject,
                                                           boolean copyMetadata) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(sourceBucket)
                    .object(sourceObject)
                    .build();

            CopyObjectArgs.Builder builder = CopyObjectArgs.builder()
                    .source(source)
                    .bucket(targetBucket)
                    .object(targetObject);

            if (copyMetadata) {
                StatObjectResponse metadata = getObjectMetadata(sourceBucket, sourceObject).get();
//                builder.headers(metadata.headers());
            }

            return this.copyObject(builder.build());
        } catch (Exception e) {
            log.error("Copy file failed: {}", e.getMessage());
            throw new RuntimeException("Copy file failed", e);
        }
    }

    /**
     * 列出所有桶
     *
     * @return List<Bucket> 桶列表
     */
    public CompletableFuture<List<Bucket>> listAllBuckets() throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException, ExecutionException, InterruptedException {
        return this.listBuckets();
    }


    /**
     * 批量删除文件
     *
     * @param bucketName  存储桶
     * @param objectNames 对象名列表
     * @return 删除结果
     */
    public List<DeleteError> deleteFiles(String bucketName, List<String> objectNames) {
        try {
            List<DeleteObject> objects = objectNames.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            Iterable<Result<DeleteError>> results = this.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objects)
                            .build());

            List<DeleteError> errors = new ArrayList<>();
            results.forEach(result -> {
                try {
                    DeleteError error = result.get();
                    if (error != null) {
                        errors.add(error);
                    }
                } catch (Exception e) {
                    log.error("Delete object failed: {}", e.getMessage());
                }
            });
            return errors;
        } catch (Exception e) {
            log.error("Batch delete files failed: {}", e.getMessage());
            throw new RuntimeException("Batch delete files failed", e);
        }
    }

}