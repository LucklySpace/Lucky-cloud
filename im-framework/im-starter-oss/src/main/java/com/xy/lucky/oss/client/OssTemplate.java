package com.xy.lucky.oss.client;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.InputStream;
import java.util.List;

/**
 * 对象存储统一接口（S3 兼容）
 * <p>
 * 屏蔽不同云厂商差异，统一提供 Bucket 与 Object 常用操作，
 * 适配所有 S3 协议兼容的对象存储服务：
 * 阿里云 OSS、腾讯云 COS、七牛云、MinIO、AWS S3 等。
 * <p>
 * 设计原则：
 * - 接口抽象：便于替换实现与扩展能力
 * - 统一规范：方法命名与语义保持一致
 * - 简洁易用：覆盖 80% 以上常见业务场景
 */
public interface OssTemplate {

    OssTemplate select(String providerName);

    void createBucket(String bucketName);

    boolean bucketExists(String bucketName);

    List<Bucket> listBuckets();

    void deleteBucket(String bucketName);

    boolean setBucketPublic(String bucketName);

    void putObject(String bucketName, String objectName, InputStream stream, String contentType) throws Exception;

    void putObject(String bucketName, String objectName, InputStream stream) throws Exception;

    S3Object getObject(String bucketName, String objectName);

    void deleteObject(String bucketName, String objectName) throws Exception;

    int deleteObjects(String bucketName, List<String> objectNames);

    void copyObject(String sourceBucket, String sourceObjectName, String targetBucket, String targetObjectName) throws Exception;

    List<S3ObjectSummary> listObjects(String bucketName, String prefix, boolean recursive);

    boolean doesObjectExist(String bucketName, String objectName);

    String getPresignedUrl(String bucketName, String objectName, int expires);

    String getPresignedPutUrl(String bucketName, String objectName, int expires);

    String getPublicUrl(String bucketName, String objectName);

    ObjectMetadata getObjectMetadata(String bucketName, String objectName);
}
