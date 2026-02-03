package com.xy.lucky.oss.client;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.InputStream;
import java.util.List;

/**
 * 对象存储服务统一操作模板接口
 * <p>
 * 基于 Amazon S3 协议，支持所有 S3 兼容的对象存储服务：
 * <ul>
 *   <li>阿里云 OSS (Alibaba Cloud OSS)</li>
 *   <li>腾讯云 COS (Tencent Cloud COS)</li>
 *   <li>七牛云对象存储 (Qiniu Cloud)</li>
 *   <li>MinIO</li>
 *   <li>AWS S3</li>
 *   <li>其他 S3 兼容服务</li>
 * </ul>
 * <p>
 * 设计原则：
 * <ul>
 *   <li>接口抽象：便于扩展和切换不同的实现</li>
 *   <li>统一规范：屏蔽不同云服务商的差异</li>
 *   <li>简洁易用：提供常用的对象存储操作</li>
 * </ul>
 *
 * @author Lucky Team
 * @since 1.0.0
 */
public interface OssTemplate {

    /**
     * 选择指定提供者的模板
     *
     * @param providerName 提供者名称
     * @return 模板实例
     */
    OssTemplate select(String providerName);

    // ==================== Bucket 操作 ====================

    /**
     * 创建存储桶（Bucket）
     * <p>
     * 如果存储桶已存在，则不执行任何操作
     *
     * @param bucketName 存储桶名称
     */
    void createBucket(String bucketName);

    /**
     * 检查存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 存储桶是否存在
     */
    boolean bucketExists(String bucketName);

    /**
     * 获取所有存储桶列表
     *
     * @return 存储桶列表
     */
    List<Bucket> listBuckets();

    /**
     * 删除存储桶
     * <p>
     * 注意：存储桶必须为空才能删除成功
     *
     * @param bucketName 存储桶名称
     */
    void deleteBucket(String bucketName);

    /**
     * 设置存储桶为公开读写
     *
     * @param bucketName 存储桶名称
     * @return 是否设置成功
     */
    boolean setBucketPublic(String bucketName);

    // ==================== 对象操作 ====================

    /**
     * 上传文件到对象存储
     *
     * @param bucketName  存储桶名称
     * @param objectName  对象名称（文件路径）
     * @param stream      文件输入流
     * @param contentType 内容类型（MIME 类型），如 "image/jpeg"
     * @throws Exception 上传失败时抛出异常
     */
    void putObject(String bucketName, String objectName, InputStream stream, String contentType) throws Exception;

    /**
     * 上传文件到对象存储（使用默认内容类型）
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param stream     文件输入流
     * @throws Exception 上传失败时抛出异常
     */
    void putObject(String bucketName, String objectName, InputStream stream) throws Exception;

    /**
     * 获取对象
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return S3 对象
     */
    S3Object getObject(String bucketName, String objectName);

    /**
     * 删除对象
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @throws Exception 删除失败时抛出异常
     */
    void deleteObject(String bucketName, String objectName) throws Exception;

    /**
     * 批量删除对象
     *
     * @param bucketName  存储桶名称
     * @param objectNames 对象名称列表
     * @return 删除成功的对象数量
     */
    int deleteObjects(String bucketName, List<String> objectNames);

    /**
     * 复制对象
     *
     * @param sourceBucket     源存储桶
     * @param sourceObjectName 源对象名称
     * @param targetBucket     目标存储桶
     * @param targetObjectName 目标对象名称
     * @throws Exception 复制失败时抛出异常
     */
    void copyObject(String sourceBucket, String sourceObjectName, String targetBucket, String targetObjectName) throws Exception;

    // ==================== 查询操作 ====================

    /**
     * 根据前缀查询对象列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象名称前缀
     * @param recursive  是否递归查询
     * @return 对象摘要列表
     */
    List<S3ObjectSummary> listObjects(String bucketName, String prefix, boolean recursive);

    /**
     * 检查对象是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 对象是否存在
     */
    boolean doesObjectExist(String bucketName, String objectName);

    // ==================== URL 操作 ====================

    /**
     * 生成对象的预签名访问 URL
     * <p>
     * 预签名 URL 包含签名信息，可以在有效期内直接访问对象，无需再次认证
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param expires    URL 有效期（单位：秒）
     * @return 预签名 URL
     */
    String getPresignedUrl(String bucketName, String objectName, int expires);

    /**
     * 生成对象的预签名上传 URL（PUT 方法）
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param expires    URL 有效期（单位：秒）
     * @return 预签名 PUT URL
     */
    String getPresignedPutUrl(String bucketName, String objectName, int expires);

    /**
     * 获取对象的公开访问 URL（不带签名）
     * <p>
     * 仅适用于公开读的存储桶或对象
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 公开访问 URL
     */
    String getPublicUrl(String bucketName, String objectName);

    // ==================== 元数据操作 ====================

    /**
     * 获取对象元数据
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 对象元数据
     */
    ObjectMetadata getObjectMetadata(String bucketName, String objectName);

    /**
     * 对象元数据信息
     */
//    class ObjectMetadata {
//        private String contentType;
//        private long contentLength;
//        private String etag;
//        private long lastModified;
//
//        public ObjectMetadata() {}
//
//        public ObjectMetadata(String contentType, long contentLength, String etag, long lastModified) {
//            this.contentType = contentType;
//            this.contentLength = contentLength;
//            this.etag = etag;
//            this.lastModified = lastModified;
//        }
//
//        public String getContentType() {
//            return contentType;
//        }
//
//        public void setContentType(String contentType) {
//            this.contentType = contentType;
//        }
//
//        public long getContentLength() {
//            return contentLength;
//        }
//
//        public void setContentLength(long contentLength) {
//            this.contentLength = contentLength;
//        }
//
//        public String getEtag() {
//            return etag;
//        }
//
//        public void setEtag(String etag) {
//            this.etag = etag;
//        }
//
//        public long getLastModified() {
//            return lastModified;
//        }
//
//        public void setLastModified(long lastModified) {
//            this.lastModified = lastModified;
//        }
//
//        @Override
//        public String toString() {
//            return "ObjectMetadata{" +
//                    "contentType='" + contentType + '\'' +
//                    ", contentLength=" + contentLength +
//                    ", etag='" + etag + '\'' +
//                    ", lastModified=" + lastModified +
//                    '}';
//        }
//    }
}
