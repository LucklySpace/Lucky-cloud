package com.xy.lucky.file.util;

import com.google.common.collect.HashMultimap;
import com.xy.lucky.file.client.MinioProperties;
import com.xy.lucky.file.client.PearlMinioClient;
import com.xy.lucky.file.domain.OssFileUploadProgress;
import com.xy.lucky.file.domain.po.OssFilePo;
import com.xy.lucky.file.domain.vo.FileChunkVo;
import com.xy.lucky.file.enums.StorageBucketEnum;
import com.xy.lucky.file.exception.FileException;
import com.xy.lucky.utils.string.StringUtils;
import io.minio.*;
import io.minio.messages.Part;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * MinIO 文件工具（精简版）
 *
 * 要点：
 * - 提供分片上传初始化、获取进度、合并、预签名 URL、断点下载等功能
 * - 内部提取小工具方法，异常统一抛 FileException
 * - 日志信息清晰，便于排查
 */
@Slf4j
@Component
public class MinioUtils {

    public static final String AVATAR_BUCKET_NAME = "avatar";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int PRESIGNED_EXPIRE_DAYS = 1;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private PearlMinioClient pearlMinioClient;

    /**
     * 获取分片上传进度：返回已上传分片信息并为未上传分片生成预签名上传 URL
     */
    public OssFileUploadProgress getMultipartUploadProgress(OssFilePo req, OssFileUploadProgress builder) {
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        String uploadId = req.getUploadId();

        try {
            log.info("查询分片上传进度 bucket={}, object={}, uploadId={}", bucket, object, uploadId);
            builder.setUploadId(uploadId);

            ListPartsResponse partsResp = pearlMinioClient.listMultipart(bucket, null, object, 1000, 0, uploadId, null, null).get();
            List<Part> uploaded = partsResp.result() == null ? Collections.emptyList() : partsResp.result().partList();

            // 所有分片编号（1..partNum）
            List<Integer> allParts = IntStream.rangeClosed(1, req.getPartNum()).boxed().toList();

            TreeMap<String, String> undone = new TreeMap<>();
            Set<Integer> finished = uploaded.stream().map(Part::partNumber).collect(HashSet::new, Set::add, Set::addAll);

            for (Integer partNumber : allParts) {
                if (!finished.contains(partNumber)) {
                    String url = generatePresignedPartUrlSafe(uploadId, bucket, object, partNumber);
                    undone.put("chunk_" + (partNumber - 1), url);
                }
            }
            builder.setUndoneChunkMap(undone);
            return builder;
        } catch (Exception e) {
            log.error("获取上传进度失败 bucket={} object={} uploadId={}", bucket, object, uploadId, e);
            throw new FileException("获取上传进度失败: " + e.getMessage());
        }
    }

    /**
     * 初始化单文件上传（返回单分片上传 URL 与 uploadId）
     */
    public FileChunkVo initUpload(OssFilePo req) {
        String bucket = getOrCreateBucketByFileName(req.getFileName());
        String object = getObjectName(generatePath(), req.getFileName());
        req.setObjectKey(object);
        req.setBucketName(bucket);

        try {
            log.info("初始化单文件上传 bucket={} object={}", bucket, object);
            String uploadId = generateUploadId(bucket, object, req.getFileType());
            String url = generatePresignedUrlSafe(bucket, object);
            return FileChunkVo.builder().uploadUrl(Map.of("chunk_0", url)).uploadId(uploadId).build();
        } catch (Exception e) {
            log.error("initUpload 失败 object={} bucket={}", object, bucket, e);
            throw new FileException("初始化分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 初始化多分片上传（返回每个分片的预签名 URL）
     */
    public FileChunkVo initMultiPartUpload(OssFilePo req) {
        String bucket = getOrCreateBucketByFileName(req.getFileName());
        String object = getObjectName(generatePath(), req.getFileName());
        req.setObjectKey(object);
        req.setBucketName(bucket);

        try {
            log.info("初始化分片上传 bucket={} object={} partNum={}", bucket, object, req.getPartNum());
            String uploadId = generateUploadId(bucket, object, req.getFileType());
            Map<String, String> urls = generatePresignedObjectUrlsSafe(uploadId, bucket, object, req.getPartNum());
            return FileChunkVo.builder().uploadUrl(urls).uploadId(uploadId).build();
        } catch (Exception e) {
            log.error("initMultiPartUpload 失败 object={} bucket={}", object, bucket, e);
            throw new FileException("初始化分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片：获取已上传分片列表并调用 completeMultipartUpload
     */
    public String mergeOssFileUpload(OssFilePo req) {
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        String uploadId = req.getUploadId();

        try {
            log.info("开始合并分片 bucket={} object={} uploadId={}", bucket, object, uploadId);

            ListPartsResponse partsResp = pearlMinioClient.listMultipart(bucket, null, object, 1000, 0, uploadId, null, null).get();
            List<Part> parts = partsResp.result() == null ? Collections.emptyList() : partsResp.result().partList();
            if (CollectionUtils.isEmpty(parts)) {
                throw new FileException("未找到已上传的分片");
            }

            // complete
            Part[] toComplete = parts.toArray(new Part[0]);
            HashMultimap<String, String> headers = HashMultimap.create();
            headers.put("Content-Type", DEFAULT_CONTENT_TYPE);
            pearlMinioClient.completeMultipartUploadAsync(bucket, null, object, uploadId, toComplete, headers, null).get();

            // 尝试清理（如果 SDK 保持分片元信息需要）
            try {
                pearlMinioClient.abortMultipartPart(bucket, null, object, uploadId, null, null);
            } catch (Exception ex) {
                log.warn("清理分片时发生错误（非阻塞） bucket={} object={} uploadId={} error={}", bucket, object, uploadId, ex.getMessage());
            }

            String path = getFilePath(bucket, object);
            log.info("合并完成 bucket={} object={} path={}", bucket, object, path);
            return path;
        } catch (Exception e) {
            log.error("合并分片失败 bucket={} object={} uploadId={}", bucket, object, uploadId, e);
            throw new FileException("合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 创建桶（简化）: 若已存在则直接返回
     */
    public String createBucket(String bucketName) {
        try {
            if (bucketExists(bucketName)) return bucketName;
            pearlMinioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            return bucketName;
        } catch (Exception e) {
            log.error("创建桶失败 bucket={}", bucketName, e);
            throw new FileException("创建桶失败: " + e.getMessage());
        }
    }

    /**
     * 检查 bucket 是否存在
     */
    public boolean bucketExists(String bucketName) {
        try {
            return pearlMinioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()).get();
        } catch (Exception e) {
            log.error("检查桶是否存在失败 bucket={}", bucketName, e);
            return false;
        }
    }

    /**
     * 生成 uploadId（内部方法，包装异常）
     */
    private String generateUploadId(String bucketName, String objectName, String contentType) {
        try {
            if (StringUtils.isBlank(contentType)) contentType = DEFAULT_CONTENT_TYPE;
            HashMultimap<String, String> headers = HashMultimap.create();
            headers.put("Content-Type", contentType);
            CreateMultipartUploadResponse resp = pearlMinioClient.createMultipartUploadAsync(bucketName, null, objectName, headers, null).get();
            return resp.result().uploadId();
        } catch (Exception e) {
            log.error("generateUploadId 失败 bucket={} object={}", bucketName, objectName, e);
            throw new FileException("初始化上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成单个预签名 PUT URL（安全包装）
     */
    private String generatePresignedUrlSafe(String bucketName, String objectName) {
        try {
            return pearlMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.PUT)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(PRESIGNED_EXPIRE_DAYS, TimeUnit.DAYS).build());
        } catch (Exception e) {
            log.error("生成预签名 URL 失败 bucket={} object={}", bucketName, objectName, e);
            throw new FileException("生成上传地址失败");
        }
    }

    /**
     * 生成分片上传预签名 URL（带 uploadId 和 partNumber 查询参数）
     */
    private String generatePresignedPartUrlSafe(String uploadId, String bucketName, String objectName, Integer partNumber) {
        try {
            return pearlMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.PUT)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(PRESIGNED_EXPIRE_DAYS, TimeUnit.DAYS)
                    .extraQueryParams(Map.of("uploadId", uploadId, "partNumber", String.valueOf(partNumber)))
                    .build());
        } catch (Exception e) {
            log.error("生成分片预签名 URL 失败 bucket={} object={} part={}", bucketName, objectName, partNumber, e);
            throw new FileException("生成分片上传地址失败");
        }
    }

    /**
     * 生成多个分片预签名 URL（安全包装）
     */
    private Map<String, String> generatePresignedObjectUrlsSafe(String uploadId, String bucketName, String objectName, Integer partCount) {
        if (partCount == null || partCount <= 0) throw new IllegalArgumentException("partCount 必须大于 0");
        Map<String, String> map = new HashMap<>(partCount);
        for (int i = 1; i <= partCount; i++) {
            map.put("chunk_" + (i - 1), generatePresignedPartUrlSafe(uploadId, bucketName, objectName, i));
        }
        return map;
    }

    /**
     * 获取对象分享（GET）预签名 URL
     */
    public String getObjectShareUrl(String bucketName, int expirySeconds, String path, String filename) {
        if (expirySeconds <= 0) expirySeconds = GetPresignedObjectUrlArgs.DEFAULT_EXPIRY_TIME;
        try {
            return pearlMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(bucketName)
                    .object(getObjectName(path, filename))
                    .expiry(expirySeconds, TimeUnit.SECONDS).build());
        } catch (Exception e) {
            log.error("获取分享链接失败 bucket={} path={} filename={}", bucketName, path, filename, e);
            throw new FileException("获取分享链接失败");
        }
    }

    /**
     * 构造对象全路径（path 可带或不带结尾 '/'）
     */
    public String getObjectName(String path, String filename) {
        if (StringUtils.isBlank(path)) return filename;
        return StringUtils.endWith(path, StringUtils.C_SLASH) ? path + filename : path + StringUtils.C_SLASH + filename;
    }

    /**
     * 生成日期路径：yyyy/MM/dd/
     */
    public String generatePath() {
        return LocalDate.now().format(DATE_FORMATTER) + "/";
    }

    /**
     * 检查对象是否存在（使用 statObject）
     */
    public boolean checkObjectExists(OssFilePo file) {
        try {
            pearlMinioClient.statObject(StatObjectArgs.builder().bucket(file.getBucketName()).object(file.getObjectKey()).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成对象访问路径（带过期签名）
     */
    public String getFilePath(String bucketName, String objectName) {
        try {
            return pearlMinioClient.generateFileUrl(bucketName, objectName, minioProperties.getLinkExpiry());
        } catch (Exception e) {
            log.error("生成文件路径失败 bucket={} object={}", bucketName, objectName, e);
            return null;
        }
    }

    /**
     * 公开文件路径（不签名）
     */
    public String getPublicFilePath(String bucketName, String objectName) {
        return StringUtils.format("{}/{}/{}", minioProperties.getEndpoint(), bucketName, objectName);
    }

    /**
     * 根据文件名选择或创建 bucket，名称规则：{year}-{code}
     */
    public String getOrCreateBucketByFileName(String fileName) {
        String year = String.valueOf(LocalDate.now().getYear());
        if (StringUtils.isBlank(fileName)) return createBucket(year + "-" + StorageBucketEnum.OTHER.getCode());

        String code = StorageBucketEnum.getBucketCodeByFilename(fileName);
        if (code == null) code = StorageBucketEnum.OTHER.getCode();

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
     * 根据文件名选择或创建 bucket，名称规则：{year}-avatar
     */
    public String getOrCreateBucketByAvatar() {
        String bucket = createBucket(LocalDate.now().format(DATE_FORMATTER) + "-" + AVATAR_BUCKET_NAME);
        setBucketPublic(bucket);
        return bucket;
    }

    public String getPresignedPartUploadUrl(String uploadId, String bucketName, String objectName, Integer partNumber) {
        return generatePresignedPartUrlSafe(uploadId, bucketName, objectName, partNumber);
    }

    /**
     * 将 bucket 设为公开（透传到 client）
     */
    public Boolean setBucketPublic(String bucketName) {
        try {
            return pearlMinioClient.setBucketPublic(bucketName);
        } catch (Exception e) {
            log.error("设置 bucket 公开失败 bucket={}", bucketName, e);
            return false;
        }
    }

    /**
     * 支持断点续传的下载（Range 可为 null）
     * 注意：在返回流之前必须先设置好所有 header，避免响应提交后再写错误响应。
     */
    public ResponseEntity<?> download(OssFilePo req, String range) {
        String bucket = req.getBucketName();
        String object = req.getObjectKey();
        String fileName = req.getFileName();

        try {
            StatObjectResponse stat = pearlMinioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(object).build()).get();
            long fileSize = stat.size();

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

            InputStream is = pearlMinioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket).object(object).offset(start).length(end - start + 1).build()).get();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName, StandardCharsets.UTF_8).build());
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
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

    /**
     * 简单封装上传（不阻塞）
     */
    public void uploadFile(String bucketName, String objectName, InputStream is, String contentType) {
        try {
            pearlMinioClient.uploadFile(bucketName, objectName, contentType, is);
            log.info("上传成功 {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("上传失败 {}/{}", bucketName, objectName, e);
            throw new FileException("上传失败: " + e.getMessage());
        }
    }
}
