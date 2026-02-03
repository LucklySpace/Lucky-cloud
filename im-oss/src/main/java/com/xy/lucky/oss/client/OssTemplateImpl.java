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
 * 对象存储服务统一操作模板实现类
 * <p>
 * 基于 Amazon S3 SDK，支持所有 S3 协议兼容的对象存储服务：
 * <ul>
 *   <li>阿里云 OSS (Alibaba Cloud OSS)</li>
 *   <li>腾讯云 COS (Tencent Cloud COS)</li>
 *   <li>七牛云对象存储 (Qiniu Cloud)</li>
 *   <li>MinIO</li>
 *   <li>AWS S3</li>
 *   <li>其他 S3 兼容服务</li>
 * </ul>
 * <p>
 * 实现特点：
 * <ul>
 *   <li>异常处理：统一捕获和转换异常</li>
 *   <li>日志记录：关键操作和异常记录日志</li>
 *   <li>参数校验：严格的参数校验</li>
 *   <li>优雅降级：合理的默认值和容错</li>
 * </ul>
 *
 * @author Lucky Team
 * @since 1.0.0
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

    // ==================== Bucket 操作 ====================

    @Override
    public void createBucket(String bucketName) {
        validateBucketName(bucketName);
        try {
            if (!amazonS3.doesBucketExistV2(bucketName)) {
                amazonS3.createBucket(bucketName);
                log.info("创建存储桶成功: {}", bucketName);
            } else {
                log.debug("存储桶已存在，跳过创建: {}", bucketName);
            }
        } catch (AmazonServiceException e) {
            log.error("创建存储桶失败: bucketName={}, error={}", bucketName, e.getMessage());
            throw new OssException("创建存储桶失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        validateBucketName(bucketName);
        try {
            return amazonS3.doesBucketExistV2(bucketName);
        } catch (AmazonServiceException e) {
            log.error("检查存储桶存在性失败: bucketName={}, error={}", bucketName, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Bucket> listBuckets() {
        try {
            return amazonS3.listBuckets();
        } catch (AmazonServiceException e) {
            log.error("获取存储桶列表失败: error={}", e.getMessage());
            throw new OssException("获取存储桶列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        validateBucketName(bucketName);
        try {
            amazonS3.deleteBucket(bucketName);
            log.info("删除存储桶成功: {}", bucketName);
        } catch (AmazonServiceException e) {
            log.error("删除存储桶失败: bucketName={}, error={}", bucketName, e.getMessage());
            throw new OssException("删除存储桶失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean setBucketPublic(String bucketName) {
        validateBucketName(bucketName);
        try {
            // 构建公开读写策略
            String policy = "{\"Version\":\"2012-10-17\","
                    + "\"Statement\":["
                    + "{\"Effect\":\"Allow\","
                    + "\"Principal\":{\"AWS\":[\"*\"]},"
                    + "\"Action\":[\"s3:GetObject\",\"s3:PutObject\",\"s3:DeleteObject\"],"
                    + "\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}"
                    + "]}";

            amazonS3.setBucketPolicy(bucketName, policy);
            log.info("设置存储桶公开访问成功: {}", bucketName);
            return true;
        } catch (AmazonServiceException e) {
            log.error("设置存储桶公开访问失败: bucketName={}, error={}", bucketName, e.getMessage());
            return false;
        }
    }

    // ==================== 对象操作 ====================

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

            log.debug("上传对象成功: {}/{}", bucketName, objectName);
        } catch (IOException e) {
            log.error("读取文件流失败: bucketName={}, objectName={}", bucketName, objectName, e);
            throw new OssException("读取文件流失败", e);
        } catch (AmazonServiceException e) {
            log.error("上传对象失败: bucketName={}, objectName={}, error={}", bucketName, objectName, e.getMessage());
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
            log.error("获取对象失败: bucketName={}, objectName={}, error={}", bucketName, objectName, e.getMessage());
            throw new OssException("获取对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectName) throws Exception {
        validateBucketAndObject(bucketName, objectName);
        try {
            amazonS3.deleteObject(bucketName, objectName);
            log.debug("删除对象成功: {}/{}", bucketName, objectName);
        } catch (AmazonServiceException e) {
            log.error("删除对象失败: bucketName={}, objectName={}, error={}", bucketName, objectName, e.getMessage());
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
            int deletedCount = result.getDeletedObjects().size();

            log.info("批量删除对象成功: bucketName={}, count={}", bucketName, deletedCount);
            return deletedCount;
        } catch (AmazonServiceException e) {
            log.error("批量删除对象失败: bucketName={}, error={}", bucketName, e.getMessage());
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
            log.info("复制对象成功: {}/{} -> {}/{}", sourceBucket, sourceObjectName, targetBucket, targetObjectName);
        } catch (AmazonServiceException e) {
            log.error("复制对象失败: source={}/{}, target={}/{}, error={}",
                    sourceBucket, sourceObjectName, targetBucket, targetObjectName, e.getMessage());
            throw new OssException("复制对象失败: " + e.getMessage(), e);
        }
    }

    // ==================== 查询操作 ====================

    @Override
    public List<S3ObjectSummary> listObjects(String bucketName, String prefix, boolean recursive) {
        validateBucketName(bucketName);
        try {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withMaxKeys(1000);

            // 如果不递归，设置分隔符
            if (!recursive) {
                request.setDelimiter("/");
            }

            ListObjectsV2Result result = amazonS3.listObjectsV2(request);
            return result.getObjectSummaries();
        } catch (AmazonServiceException e) {
            log.error("查询对象列表失败: bucketName={}, prefix={}, error={}", bucketName, prefix, e.getMessage());
            throw new OssException("查询对象列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName) {
        validateBucketAndObject(bucketName, objectName);
        try {
            return amazonS3.doesObjectExist(bucketName, objectName);
        } catch (AmazonServiceException e) {
            log.error("检查对象存在性失败: bucketName={}, objectName={}, error={}", bucketName, objectName, e.getMessage());
            return false;
        }
    }

    // ==================== URL 操作 ====================

    @Override
    public String getPresignedUrl(String bucketName, String objectName, int expires) {
        validateBucketAndObject(bucketName, objectName);
        if (expires <= 0) {
            expires = ossProperties.getPresignedUrlExpiry();
        }

        try {
            Date expiration = calculateExpiration(expires);
            URL url = amazonS3.generatePresignedUrl(bucketName, objectName, expiration);
            log.debug("生成预签名 URL 成功: {}/{}", bucketName, objectName);
            return url.toString();
        } catch (SdkClientException e) {
            log.error("生成预签名 URL 失败: bucketName={}, objectName={}", bucketName, objectName, e);
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
            log.debug("生成预签名 PUT URL 成功: {}/{}", bucketName, objectName);
            return url.toString();
        } catch (SdkClientException e) {
            log.error("生成预签名 PUT URL 失败: bucketName={}, objectName={}", bucketName, objectName, e);
            throw new OssException("生成预签名 PUT URL 失败", e);
        }
    }

    @Override
    public String getPublicUrl(String bucketName, String objectName) {
        validateBucketAndObject(bucketName, objectName);
        String endpoint = ossProperties.getEndpoint();

        // 根据 path-style 模式生成 URL
        if (Boolean.TRUE.equals(ossProperties.getPathStyleAccess())) {
            // path-style: http://endpoint/bucket/object
            return String.format("%s/%s/%s", endpoint, bucketName, objectName);
        } else {
            // virtual-hosted-style: http://bucket.endpoint/object
            return String.format("%s.%s/%s", bucketName, endpoint, objectName);
        }
    }

    // ==================== 元数据操作 ====================

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String objectName) {
        validateBucketAndObject(bucketName, objectName);
        try {
            return amazonS3.getObjectMetadata(bucketName, objectName);
        } catch (AmazonServiceException e) {
            log.error("获取对象元数据失败: bucketName={}, objectName={}, error={}", bucketName, objectName, e.getMessage());
            throw new OssException("获取对象元数据失败: " + e.getMessage(), e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 计算过期时间
     *
     * @param seconds 过期秒数
     * @return 过期日期
     */
    private Date calculateExpiration(int seconds) {
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    /**
     * 校验存储桶名称
     *
     * @param bucketName 存储桶名称
     */
    private void validateBucketName(String bucketName) {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalArgumentException("存储桶名称不能为空");
        }
    }

    /**
     * 校验存储桶和对象名称
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    private void validateBucketAndObject(String bucketName, String objectName) {
        validateBucketName(bucketName);
        if (!StringUtils.hasText(objectName)) {
            throw new IllegalArgumentException("对象名称不能为空");
        }
    }

    /**
     * 对象存储操作异常
     */
    public static class OssException extends RuntimeException {
        public OssException(String message) {
            super(message);
        }

        public OssException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
