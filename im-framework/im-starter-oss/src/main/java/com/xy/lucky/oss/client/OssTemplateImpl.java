package com.xy.lucky.oss.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Amazon S3 SDK 的对象存储实现
 * <p>
 * 特点：
 * - 统一异常：捕获并转换 SDK 异常为业务异常
 * - 兼容模式：支持 path-style 与虚拟域名两种访问样式
 * - 预签名：提供 GET/PUT 预签名 URL 生成
 * - 元数据：支持对象元数据读取
 */
@Slf4j
@RequiredArgsConstructor
public class OssTemplateImpl implements OssTemplate {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private final AmazonS3 amazonS3;
    private final OssProperties ossProperties;

    @Override
    public OssTemplate select(String providerName) {
        return this;
    }

    @Override
    public void createBucket(String bucketName) {
        validateBucketName(bucketName);
        try {
            if (!amazonS3.doesBucketExistV2(bucketName)) {
                amazonS3.createBucket(bucketName);
                log.info("创建存储桶成功: {}", bucketName);
            }
        } catch (AmazonServiceException e) {
            throw new OssException("创建存储桶失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        validateBucketName(bucketName);
        try {
            return amazonS3.doesBucketExistV2(bucketName);
        } catch (AmazonServiceException e) {
            return false;
        }
    }

    @Override
    public List<Bucket> listBuckets() {
        try {
            return amazonS3.listBuckets();
        } catch (AmazonServiceException e) {
            throw new OssException("获取存储桶列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        validateBucketName(bucketName);
        try {
            amazonS3.deleteBucket(bucketName);
        } catch (AmazonServiceException e) {
            throw new OssException("删除存储桶失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean setBucketPublic(String bucketName) {
        validateBucketName(bucketName);
        try {
            String policy = "{\"Version\":\"2012-10-17\","
                    + "\"Statement\":["
                    + "{\"Effect\":\"Allow\","
                    + "\"Principal\":{\"AWS\":[\"*\"]},"
                    + "\"Action\":[\"s3:GetObject\",\"s3:PutObject\",\"s3:DeleteObject\"],"
                    + "\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}"
                    + "]}";
            amazonS3.setBucketPolicy(bucketName, policy);
            return true;
        } catch (AmazonServiceException e) {
            return false;
        }
    }

    @Override
    public void putObject(String bucketName, String objectName, InputStream stream, String contentType) throws Exception {
        validateBucketAndObject(bucketName, objectName);
        if (!StringUtils.hasText(contentType)) {
            contentType = DEFAULT_CONTENT_TYPE;
        }
        try {
            byte[] bytes = IOUtils.toByteArray(stream);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(contentType);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            amazonS3.putObject(bucketName, objectName, byteArrayInputStream, metadata);
        } catch (IOException e) {
            throw new OssException("读取文件流失败", e);
        } catch (AmazonServiceException e) {
            throw new OssException("上传对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
        putObject(bucketName, objectName, stream, DEFAULT_CONTENT_TYPE);
    }

    @Override
    public S3Object getObject(String bucketName, String objectName) {
        validateBucketAndObject(bucketName, objectName);
        try {
            return amazonS3.getObject(bucketName, objectName);
        } catch (AmazonServiceException e) {
            throw new OssException("获取对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectName) throws Exception {
        validateBucketAndObject(bucketName, objectName);
        try {
            amazonS3.deleteObject(bucketName, objectName);
        } catch (AmazonServiceException e) {
            throw new OssException("删除对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteObjects(String bucketName, List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return 0;
        }
        validateBucketName(bucketName);
        try {
            List<DeleteObjectsRequest.KeyVersion> keysToDelete = objectNames.stream()
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .collect(Collectors.toList());
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName)
                    .withKeys(keysToDelete)
                    .withQuiet(false);
            DeleteObjectsResult result = amazonS3.deleteObjects(request);
            return result.getDeletedObjects().size();
        } catch (AmazonServiceException e) {
            throw new OssException("批量删除对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void copyObject(String sourceBucket, String sourceObjectName, String targetBucket, String targetObjectName) throws Exception {
        validateBucketAndObject(sourceBucket, sourceObjectName);
        validateBucketAndObject(targetBucket, targetObjectName);
        try {
            CopyObjectRequest request = new CopyObjectRequest(sourceBucket, sourceObjectName, targetBucket, targetObjectName);
            amazonS3.copyObject(request);
        } catch (AmazonServiceException e) {
            throw new OssException("复制对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<S3ObjectSummary> listObjects(String bucketName, String prefix, boolean recursive) {
        validateBucketName(bucketName);
        try {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withMaxKeys(1000);
            if (!recursive) {
                request.setDelimiter("/");
            }
            ListObjectsV2Result result = amazonS3.listObjectsV2(request);
            return result.getObjectSummaries();
        } catch (AmazonServiceException e) {
            throw new OssException("查询对象列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName) {
        validateBucketAndObject(bucketName, objectName);
        try {
            return amazonS3.doesObjectExist(bucketName, objectName);
        } catch (AmazonServiceException e) {
            return false;
        }
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectName, int expires) {
        validateBucketAndObject(bucketName, objectName);
        if (expires <= 0) {
            expires = ossProperties.getPresignedUrlExpiry();
        }
        try {
            Date expiration = calculateExpiration(expires);
            URL url = amazonS3.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();
        } catch (SdkClientException e) {
            throw new OssException("生成预签名 URL 失败", e);
        }
    }

    @Override
    public String getPresignedPutUrl(String bucketName, String objectName, int expires) {
        validateBucketAndObject(bucketName, objectName);
        if (expires <= 0) {
            expires = ossProperties.getPresignedUrlExpiry();
        }
        try {
            Date expiration = calculateExpiration(expires);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName)
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(expiration);
            URL url = amazonS3.generatePresignedUrl(request);
            return url.toString();
        } catch (SdkClientException e) {
            throw new OssException("生成预签名 PUT URL 失败", e);
        }
    }

    @Override
    public String getPublicUrl(String bucketName, String objectName) {
        validateBucketAndObject(bucketName, objectName);
        String endpoint = ossProperties.getEndpoint();
        if (Boolean.TRUE.equals(ossProperties.getPathStyleAccess())) {
            return String.format("%s/%s/%s", endpoint, bucketName, objectName);
        } else {
            return String.format("%s.%s/%s", bucketName, endpoint, objectName);
        }
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String objectName) {
        validateBucketAndObject(bucketName, objectName);
        try {
            return amazonS3.getObjectMetadata(bucketName, objectName);
        } catch (AmazonServiceException e) {
            throw new OssException("获取对象元数据失败: " + e.getMessage(), e);
        }
    }

    private Date calculateExpiration(int seconds) {
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    private void validateBucketName(String bucketName) {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalArgumentException("存储桶名称不能为空");
        }
    }

    private void validateBucketAndObject(String bucketName, String objectName) {
        validateBucketName(bucketName);
        if (!StringUtils.hasText(objectName)) {
            throw new IllegalArgumentException("对象名称不能为空");
        }
    }

    public static class OssException extends RuntimeException {
        public OssException(String message) {
            super(message);
        }

        public OssException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
