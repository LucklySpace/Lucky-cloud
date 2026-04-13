package com.xy.lucky.oss.util;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.xy.lucky.oss.client.OssProperties;
import com.xy.lucky.oss.client.OssTemplate;
import com.xy.lucky.oss.domain.OssFileUploadProgress;
import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.domain.vo.FileChunkVo;
import com.xy.lucky.oss.enums.StorageBucketEnum;
import com.xy.lucky.oss.exception.FileException;
import com.xy.lucky.utils.string.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对象存储服务统一工具类
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
 * 功能特性：
 * <ul>
 *   <li>分片上传：支持大文件分片上传和断点续传</li>
 *   <li>预签名 URL：生成带签名的上传/下载 URL</li>
 *   <li>断点下载：支持 Range 断点续传</li>
 *   <li>智能分桶：根据文件类型自动选择存储桶</li>
 *   <li>优雅降级：异常统一处理和日志记录</li>
 * </ul>
 *
 */
@Slf4j
@Component
public class OssUtils {

    public static final String AVATAR_BUCKET_NAME = "avatar";
    public static final String AUDIO_BUCKET_NAME = "audio";
    public static final String IMAGE_BUCKET_NAME = "image";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int PRESIGNED_EXPIRE_DAYS = 1;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    @Resource
    private OssTemplate ossTemplate;

    @Resource
    private OssProperties ossProperties;

    // ==================== 分片上传 ====================

    /**
     * 初始化单文件上传（返回单分片上传 URL 与 uploadId）
     * <p>
     * 适用于小文件上传，直接使用 PUT 方法上传整个文件
     *
     * @param req 上传请求参数
     * @return 上传配置信息（包含上传 URL 和 uploadId）
     */
    public FileChunkVo initUpload(OssFilePo req) {
        String bucket = getOrCreateBucketByFileName(req.getFileName());
        String object = getObjectName(generatePath(), req.getFileName());
        String uploadId = UUID.randomUUID().toString();
        req.setObjectKey(object);
        req.setBucketName(bucket);
        req.setUploadId(uploadId);

        try {
            log.info("初始化单文件上传 bucket={} object={}", bucket, object);

            // 生成预签名 PUT URL
            String url = ossTemplate.getPresignedPutUrl(bucket, object, ossProperties.getPresignedUrlExpiry());

            return FileChunkVo.builder()
                    .uploadUrl(Map.of("1", url))
                    .uploadId(uploadId)
                    .build();
        } catch (Exception e) {
            log.error("initUpload 失败 object={} bucket={}", object, bucket, e);
            throw new FileException("初始化上传失败: " + e.getMessage());
        }
    }

    /**
     * 初始化多分片上传
     * <p>
     * 适用于大文件分片上传，支持断点续传
     * <p>
     * 注意：Amazon S3 SDK 的分片上传实现较为复杂，这里简化为直接使用预签名 URL
     * 如需真正的分片上传，建议使用 MinIO SDK 或 AWS SDK 的高级 API
     *
     * @param req 上传请求参数
     * @return 分片上传配置信息
     */
    public FileChunkVo initMultiPartUpload(OssFilePo req) {
        Integer partNum = req.getPartNum();
        if (partNum == null || partNum < 1) {
            throw new FileException("分片数量必须大于0");
        }
        String bucket = getOrCreateBucketByFileName(req.getFileName());
        String object = getObjectName(generatePath(), req.getFileName());
        req.setObjectKey(object);
        req.setBucketName(bucket);

        try {
            String uploadId = ossTemplate.initiateMultipartUpload(bucket, object, req.getContentType());
            req.setUploadId(uploadId);
            log.info("初始化分片上传 bucket={} object={} partNum={}", bucket, object, partNum);

            Map<String, String> urls = new LinkedHashMap<>(partNum);

            for (int partNumber = 1; partNumber <= partNum; partNumber++) {
                String url = ossTemplate.getPresignedUploadPartUrl(bucket, object, uploadId, partNumber, ossProperties.getPresignedUrlExpiry());
                urls.put(String.valueOf(partNumber), url);
            }

            return FileChunkVo.builder()
                    .uploadUrl(urls)
                    .uploadId(uploadId)
                    .build();
        } catch (Exception e) {
            log.error("initMultiPartUpload 失败 object={} bucket={}", object, bucket, e);
            throw new FileException("初始化分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片（简化实现）
     * <p>
     * 注意：这是简化实现，真正的 S3 分片合并需要使用 completeMultipartUpload API
     * 这里假设分片已经上传为独立对象，需要将它们合并
     *
     * @param req 上传请求参数
     * @return 文件访问路径
     */
    public String mergeOssFileUpload(OssFilePo req) {
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        String uploadId = req.getUploadId();
        Integer partNum = req.getPartNum();

        try {
            log.info("开始合并分片 bucket={} object={} uploadId={}", bucket, object, uploadId);

            if (partNum == null || partNum < 1) {
                throw new FileException("分片信息异常");
            }

            if (partNum == 1) {
                if (!ossTemplate.doesObjectExist(bucket, object)) {
                    throw new FileException("文件尚未上传完成");
                }
            } else {
                List<Integer> uploadedParts = ossTemplate.listUploadedParts(bucket, object, uploadId);
                TreeMap<String, String> undone = getPendingChunkUploadUrls(req, uploadedParts);
                if (!undone.isEmpty()) {
                    throw new FileException("仍有分片未上传完成，剩余分片数=" + undone.size());
                }
                ossTemplate.completeMultipartUpload(bucket, object, uploadId);
            }

            String path = getPresignedGetUrl(bucket, object);
            if (path == null) {
                throw new FileException("生成文件访问地址失败");
            }
            log.info("合并完成 bucket={} object={} path={}", bucket, object, path);
            return path;

        } catch (Exception e) {
            log.error("合并分片失败 bucket={} object={} uploadId={}", bucket, object, uploadId, e);
            throw new FileException("合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 获取分片上传进度
     * <p>
     * 查询已上传的分片，为未上传的分片生成预签名 URL
     *
     * @param req     上传请求参数
     * @param builder 上传进度构建器
     * @return 上传进度信息
     */
    public OssFileUploadProgress getMultipartUploadProgress(OssFilePo req, OssFileUploadProgress builder) {
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        String uploadId = req.getUploadId();
        Integer partNum = req.getPartNum();

        try {
            log.info("查询分片上传进度 bucket={} object={} uploadId={}", bucket, object, uploadId);
            builder.setUploadId(uploadId);

            if (partNum == null || partNum < 1) {
                throw new FileException("分片信息异常");
            }

            if (partNum == 1) {
                TreeMap<String, String> undone = new TreeMap<>();
                if (!ossTemplate.doesObjectExist(bucket, object)) {
                    String url = ossTemplate.getPresignedPutUrl(bucket, object, ossProperties.getPresignedUrlExpiry());
                    undone.put("1", url);
                }
                builder.setUndoneChunkMap(undone);
                return builder;
            }

            List<Integer> uploadedParts = ossTemplate.listUploadedParts(bucket, object, uploadId);
            TreeMap<String, String> undone = getPendingChunkUploadUrls(req, uploadedParts);
            builder.setUndoneChunkMap(undone);
            return builder;
        } catch (Exception e) {
            log.error("获取上传进度失败 bucket={} object={} uploadId={}", bucket, object, uploadId, e);
            throw new FileException("获取上传进度失败: " + e.getMessage());
        }
    }

    // ==================== Bucket 管理 ====================

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     * @return 创建的存储桶名称
     */
    public String createBucket(String bucketName) {
        try {
            if (ossTemplate.bucketExists(bucketName)) {
                return bucketName;
            }
            ossTemplate.createBucket(bucketName);
            return bucketName;
        } catch (Exception e) {
            log.error("创建桶失败 bucket={}", bucketName, e);
            throw new FileException("创建桶失败: " + e.getMessage());
        }
    }

    /**
     * 检查存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    public boolean bucketExists(String bucketName) {
        try {
            return ossTemplate.bucketExists(bucketName);
        } catch (Exception e) {
            log.error("检查桶是否存在失败 bucket={}", bucketName, e);
            return false;
        }
    }

    /**
     * 设置存储桶为公开访问
     *
     * @param bucketName 存储桶名称
     * @return 是否设置成功
     */
    public Boolean setBucketPublic(String bucketName) {
        try {
            return ossTemplate.setBucketPublic(bucketName);
        } catch (Exception e) {
            log.error("设置 bucket 公开失败 bucket={}", bucketName, e);
            return false;
        }
    }

    // ==================== 文件上传/下载 ====================

    /**
     * 上传文件
     *
     * @param bucketName  存储桶名称
     * @param objectName  对象名称
     * @param is          输入流
     * @param contentType 内容类型
     */
    public void uploadFile(String bucketName, String objectName, InputStream is, String contentType) {
        try {
            ossTemplate.putObject(bucketName, objectName, is, contentType);
            log.info("上传成功 {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("上传失败 {}/{}", bucketName, objectName, e);
            throw new FileException("上传失败: " + e.getMessage());
        }
    }

    /**
     * 支持断点续传的下载
     * <p>
     * 支持 HTTP Range 请求，实现断点续传
     *
     * @param req   文件请求参数
     * @param range Range 头（如 "bytes=0-1023"）
     * @return 文件响应
     */
    public ResponseEntity<?> download(OssFilePo req, String range) {
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        String fileName = req.getFileName();

        try {
            // 获取对象元数据
            ObjectMetadata metadata = ossTemplate.getObjectMetadata(bucket, object);
            long fileSize = metadata.getContentLength();

            long start = 0, end = fileSize - 1;
            boolean hasRange = StringUtils.isNotBlank(range) && range.startsWith("bytes=");

            if (hasRange) {
                String[] parts = range.replace("bytes=", "").split("-");
                start = Long.parseLong(parts[0]);
                if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
                    end = Long.parseLong(parts[1]);
                }
            }

            if (start < 0 || end >= fileSize || start > end) {
                log.warn("Invalid Range header: {} for file size {}", range, fileSize);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }

            // 获取对象内容
            S3Object s3Object = ossTemplate.getObject(bucket, object);
            InputStream is = s3Object.getObjectContent();

            // 跳到起始位置
            is.skip(start);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build());
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.CONTENT_TYPE, metadata.getContentType());
            headers.setContentLength(end - start + 1L);

            if (hasRange) {
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            }

            HttpStatus status = hasRange ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).headers(headers).body(new InputStreamResource(is));

        } catch (IllegalArgumentException iae) {
            log.warn("下载参数非法 bucket={} object={} range={}", bucket, object, range, iae);
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        } catch (Exception e) {
            log.error("下载异常 bucket={} object={} range={}", bucket, object, range, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== URL 生成 ====================

    /**
     * 生成对象访问路径（带过期签名）
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 预签名 URL
     */
    public String getPresignedGetUrl(String bucketName, String objectName) {
        try {
            return ossTemplate.getPresignedUrl(bucketName, objectName, ossProperties.getPresignedUrlExpiry());
        } catch (Exception e) {
            log.error("生成文件路径失败 bucket={} object={}", bucketName, objectName, e);
            return null;
        }
    }

    /**
     * 获取对象公开访问路径（带签名）
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param expires  过期时间
     * @return 签名 URL
     */
    public String getPresignedUrl(String bucketName, String objectName, Integer expires) {
        return ossTemplate.getPresignedUrl(bucketName, objectName, expires);
    }

    public void abortMultipartUpload(OssFilePo req) {
        if (req == null || req.getPartNum() == null || req.getPartNum() <= 1) {
            return;
        }
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        String uploadId = req.getUploadId();
        if (StringUtils.isBlank(bucket) || StringUtils.isBlank(object) || StringUtils.isBlank(uploadId)) {
            return;
        }
        try {
            ossTemplate.abortMultipartUpload(bucket, object, uploadId);
        } catch (Exception e) {
            throw new FileException("取消分片上传失败: " + e.getMessage());
        }
    }

    private TreeMap<String, String> getPendingChunkUploadUrls(OssFilePo req, List<Integer> uploadedParts) {
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        int partNum = req.getPartNum();
        Set<Integer> uploadedPartSet = uploadedParts == null ? Set.of() : uploadedParts.stream().collect(Collectors.toSet());
        TreeMap<String, String> undone = new TreeMap<>();
        for (int partNumber = 1; partNumber <= partNum; partNumber++) {
            if (!uploadedPartSet.contains(partNumber)) {
                String url = ossTemplate.getPresignedUploadPartUrl(bucket, object, req.getUploadId(), partNumber, ossProperties.getPresignedUrlExpiry());
                undone.put(String.valueOf(partNumber), url);
            }
        }
        return undone;
    }

    // ==================== 工具方法 ====================

    /**
     * 构造对象全路径（path 可带或不带结尾 '/'）
     *
     * @param path     路径
     * @param filename 文件名
     * @return 对象名称
     */
    public String getObjectName(String path, String filename) {
        if (StringUtils.isBlank(path)) {
            return filename;
        }
        return StringUtils.endWith(path, StringUtils.C_SLASH) ? path + filename : path + StringUtils.C_SLASH + filename;
    }

    /**
     * 生成日期路径：yyyy/MM/dd/
     *
     * @return 日期路径
     */
    public String generatePath() {
        return LocalDate.now().format(DATE_FORMATTER) + "/";
    }

    /**
     * 检查对象是否存在
     *
     * @param file 文件信息
     * @return 是否存在
     */
    public boolean checkObjectExists(OssFilePo file) {
        try {
            return ossTemplate.doesObjectExist(file.getBucketName(), file.getObjectKey());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据文件名选择或创建存储桶
     * <p>
     * 存储桶命名规则：{year}-{code}
     *
     * @param fileName 文件名
     * @return 存储桶名称
     */
    public String getOrCreateBucketByFileName(String fileName) {
        String year = String.valueOf(LocalDate.now().getYear());
        if (StringUtils.isBlank(fileName)) {
            return createBucket(year + "-" + StorageBucketEnum.OTHER.getCode());
        }

        String code = StorageBucketEnum.getBucketCodeByFilename(fileName);
        if (code == null) {
            code = StorageBucketEnum.OTHER.getCode();
        }

        // 特殊处理 avatar/thumbnail
        if ("thumbnail".equals(code)) {
            String bucket = createBucket(year + "-" + code);
            setBucketPublic(bucket);
            return bucket;
        }

        // code 可能为 mime 类型（包含 '/'), 优先取主类型
        if (code.contains("/")) {
            String main = code.substring(0, code.indexOf('/'));
            if (StorageBucketEnum.fromCode(main) != null) {
                return createBucket(year + "-" + main);
            }
        }

        return createBucket(year + "-" + code.toLowerCase());
    }

    /**
     * 根据桶编码选择或创建存储桶。
     *
     * @param bucketCode 桶编码
     * @return 存储桶名称
     */
    public String getOrCreateBucketByCode(String bucketCode) {
        String year = String.valueOf(LocalDate.now().getYear());
        String code = StringUtils.isBlank(bucketCode) ? StorageBucketEnum.OTHER.getCode() : bucketCode.trim().toLowerCase();
        if ("thumbnail".equals(code)) {
            String bucket = createBucket(year + "-" + code);
            setBucketPublic(bucket);
            return bucket;
        }
        return createBucket(year + "-" + code);
    }

    /**
     * 创建头像存储桶
     * <p>
     * 存储桶命名规则：{year}-avatar
     *
     * @return 存储桶名称
     */
    public String getOrCreateBucketByAvatar() {

        String bucket = createBucket(LocalDate.now().getYear() + "-" + LocalDate.now().getMonthValue() + "-" + AVATAR_BUCKET_NAME);

        return bucket;
    }

    /**
     * 创建语音存储桶
     * <p>
     * 存储桶命名规则：{year}-{month}-audio
     *
     * @return 存储桶名称
     */
    public String getOrCreateBucketByAudio() {

        return createBucket(LocalDate.now().getYear() + "-" + LocalDate.now().getMonthValue() + "-" + AUDIO_BUCKET_NAME);
    }

    /**
     * 创建图片存储桶
     * <p>
     * 存储桶命名规则：{year}-{month}-image
     *
     * @return 存储桶名称
     */
    public String getOrCreateBucketByImage() {

        return createBucket(LocalDate.now().getYear() + "-" + LocalDate.now().getMonthValue() + "-" + IMAGE_BUCKET_NAME);
    }

    /**
     * 获取文件后缀
     *
     * @param fileName 文件名
     * @return 文件后缀
     */
    public String getFileSuffix(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        return index > 0 ? fileName.substring(index + 1) : null;
    }
}
