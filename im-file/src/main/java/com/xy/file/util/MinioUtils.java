package com.xy.file.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.HashMultimap;
import com.xy.file.client.MinioProperties;
import com.xy.file.client.PearlMinioClient;
import com.xy.file.entity.OssFile;
import com.xy.file.entity.OssFileUploadProgress;
import com.xy.file.enums.StorageBucketEnum;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Part;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.xy.file.util.ResultCode.UPLOAD_FILE_FAILED;

/**
 * MinIO 文件存储工具类
 * <p>
 * 提供以下主要功能:
 * 1. 文件上传(支持单文件和分片上传)
 * 2. 文件下载
 * 3. 文件分享链接生成
 * 4. 存储桶(Bucket)管理
 *
 * @author Your Name
 */
@Slf4j
@Component
public class MinioUtils {

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private RedisRepo redisRepo; // Redis 数据库存储

    @Resource
    private PearlMinioClient pearlMinioClient; // 自定义的 Minio 客户端

    /**
     * 获取分片上传进度
     *
     * @param reqOssFile            文件信息对象
     * @param uploadProgressBuilder 上传进度构建器
     * @return 包含已上传分片信息和未上传分片预签名URL的响应
     */
    public ResponseResult getMultipartUploadProgress(OssFile reqOssFile, OssFileUploadProgress uploadProgressBuilder) {
        String bucketName = reqOssFile.getBucketName();
        String fileName = reqOssFile.getFileName();
        String uploadId = reqOssFile.getUploadId();

        try {
            log.info("查询分片上传进度 - bucket:{}, file:{}, uploadId:{}", bucketName, fileName, uploadId);
            uploadProgressBuilder.setUploadId(uploadId);

            // 获取已上传的分片列表
            ListPartsResponse partResult = pearlMinioClient.listMultipart(
                    bucketName, null, fileName, 1000, 0, uploadId, null, null
            ).get();

            if (partResult.result() != null) {
                // 存储未完成分片的预签名URL
                TreeMap<String, String> chunkMap = new TreeMap<>();

                // 获取所有分片编号列表
                List<Integer> allPartNumbers = IntStream.rangeClosed(1, reqOssFile.getPartNum())
                        .boxed()
                        .toList();

                List<Part> uploadedParts = partResult.result().partList();

                if (CollUtil.isEmpty(uploadedParts)) {
                    // 没有已上传的分片,生成所有分片的预签名URL
                    for (Integer partNumber : allPartNumbers) {
                        String uploadUrl = generatePresignedObjectUrl(uploadId, bucketName, fileName, partNumber);
                        chunkMap.put("chunk_" + (partNumber - 1), uploadUrl);
                    }
                } else {
                    // 获取已上传完成的分片编号
                    List<Integer> finishedPartNumbers = uploadedParts.stream()
                            .map(Part::partNumber)
                            .collect(Collectors.toList());

                    // 为未上传的分片生成预签名URL
                    for (Integer partNumber : allPartNumbers) {
                        if (!finishedPartNumbers.contains(partNumber)) {
                            String uploadUrl = generatePresignedObjectUrl(uploadId, bucketName, fileName, partNumber);
                            chunkMap.put("chunk_" + (partNumber - 1), uploadUrl);
                        }
                    }
                }
                uploadProgressBuilder.setUndoneChunkMap(chunkMap);
            }

            return ResponseResult.success(uploadProgressBuilder);

        } catch (Exception e) {
            log.error("获取上传进度失败 - bucket:{}, file:{}", bucketName, fileName, e);
            return ResponseResult.error(UPLOAD_FILE_FAILED);
        }
    }

    /**
     * 初始化单文件上传
     *
     * @param reqOssFile 文件信息对象,包含文件名、类型等信息
     * @return 包含上传URL和uploadId的响应结果
     */
    public ResponseResult initUpload(OssFile reqOssFile) {

        // 获取文件桶名
        String bucketName = getOrCreateBucketByFileType(reqOssFile.getFileType());

        // 自动生成路径和文件名
        String objectName = this.getObjectName(this.generatePath(), reqOssFile.getFileName());

        try {

            log.info("tip message: 通过 <{}-{}> 开始单文件上传<minio>", objectName, bucketName);

            String uploadId = generateUploadId(bucketName, objectName, reqOssFile.getFileType());

            reqOssFile.setUploadId(uploadId);

            reqOssFile.setBucketName(bucketName);

            // 获取每个分片的预签名上传地址
            String url = generatePresignedObjectUrl(bucketName, objectName);

            return ResponseResult.success(Map.of("uploadUrl", Map.of("chunk_" + 0, url), "uploadId", uploadId));

        } catch (Exception e) {

            log.error("error message: 初始化分片上传失败、原因:", e);

            // 返回 文件上传失败
            return ResponseResult.error(UPLOAD_FILE_FAILED);
        }
    }

    /**
     * 初始化分片上传
     *
     * @param reqOssFile 文件信息对象,包含文件名、分片数量等信息
     * @return 包含分片上传URL列表和uploadId的响应结果
     */
    public ResponseResult initMultiPartUpload(OssFile reqOssFile) {
        // 获取文件桶名
        String bucketName = getOrCreateBucketByFileType(reqOssFile.getFileType());

        // 自动生成路径和文件名
        String objectName = this.getObjectName(this.generatePath(), reqOssFile.getFileName());

        try {
            log.info("tip message: 通过 <{}-{}> 开始分片上传<minio>", objectName, bucketName);

            // 获取 单文件上传
            String uploadId = generateUploadId(bucketName, objectName, reqOssFile.getFileType());

            reqOssFile.setUploadId(uploadId);

            reqOssFile.setBucketName(bucketName);

            // 获取每个分片的预签名上传地址
            Map<String, String> urlsMap = generatePresignedObjectUrls(uploadId, bucketName, objectName, reqOssFile.getPartNum());

            return ResponseResult.success(Map.of("uploadUrl", urlsMap, "uploadId", uploadId));

        } catch (Exception e) {

            log.error("error message: 初始化分片上传失败、原因:", e);

            // 返回 文件上传失败
            return ResponseResult.error(UPLOAD_FILE_FAILED);
        }
    }


    /**
     * 合并分片上传的文件
     *
     * @param reqOssFile 文件信息对象,包含uploadId等信息
     * @return 合并结果, 成功则返回文件访问路径
     */
    public ResponseResult mergeOssFileUpload(OssFile reqOssFile) {

        String bucketName = reqOssFile.getBucketName();
        String fileName = reqOssFile.getFileName();
        String uploadId = reqOssFile.getUploadId();

        // 自动生成路径和文件名
        String objectName = this.getObjectName(this.generatePath(), reqOssFile.getFileName());

        try {
            log.info("开始合并文件分片 - bucket:{}, file:{}, uploadId:{}", bucketName, objectName, uploadId);

            // 1. 获取所有分片信息
            ListPartsResponse partResult = pearlMinioClient.listMultipart(
                    bucketName,
                    null,
                    objectName,
                    1000,
                    0,
                    uploadId,
                    null,
                    null
            ).get();

            List<Part> partList = partResult.result().partList();
            if (partList.isEmpty()) {
                log.warn("未找到文件分片 - bucket:{}, file:{}, uploadId:{}", bucketName, objectName, uploadId);
                return ResponseResult.error("未找到文件分片");
            }

            // 2. 合并分片
            Part[] parts = partList.toArray(new Part[0]);

            pearlMinioClient.completeMultipartUploadAsync(
                    bucketName,
                    null,
                    objectName,
                    uploadId,
                    parts,
                    null,
                    null
            );

            // 3. 清理分片
            try {
                pearlMinioClient.abortMultipartPart(
                        bucketName,
                        null,
                        objectName,
                        uploadId,
                        null,
                        null
                );
                log.info("成功清理文件分片 - bucket:{}, file:{}, uploadId:{}", bucketName, objectName, uploadId);
            } catch (Exception e) {
                // 清理分片失败不影响主流程，只记录警告日志
                log.warn("清理文件分片失败 - bucket:{}, file:{}, uploadId:{}, error:{}",
                        bucketName, objectName, uploadId, e.getMessage());
            }

            String filePath = this.getFilePath(bucketName, objectName);

            log.info("文件合并完成 - bucket:{}, file:{}", bucketName, objectName);

            return ResponseResult.success(Map.of("url", filePath));

        } catch (Exception e) {
            log.error("文件合并失败 - bucket:{}, file:{}, uploadId:{}", bucketName, objectName, uploadId, e);
            return ResponseResult.error("文件合并失败: " + e.getMessage());
        }
    }

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     * @return 创建的存储桶名称
     * @throws RuntimeException 创建失败时抛出异常
     */
    public String createBucket(String bucketName) {
        try {
            // 检查桶是否已存在
            if (pearlMinioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()).get()) {
                return bucketName; // 如果桶存在，返回桶名称
            }
            pearlMinioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build()); // 创建新桶
            return bucketName; // 返回新创建的桶名称
        } catch (Exception e) {
            log.error("创建桶 <{}> 时发生错误", bucketName, e); // 异常日志
            throw new RuntimeException(e); // 抛出运行时异常
        }
    }


    /**
     * 生成上传 id。
     *
     * @param bucketName  桶名称
     * @param objectName  文件全路径名称
     * @param contentType 文件类型
     * @return 上传 ID
     */
    private String generateUploadId(String bucketName, String objectName, String contentType) throws Exception {
        if (CharSequenceUtil.isBlank(contentType)) {
            contentType = "application/octet-stream"; // 默认文件类型
        }
        HashMultimap<String, String> headers = HashMultimap.create();
        headers.put("Content-Type", contentType); // 设置文件类型

        CreateMultipartUploadResponse createMultipartUploadResponse = pearlMinioClient.createMultipartUploadAsync(bucketName, null, objectName, headers, null).get();// 初始化分片上传并返回上传 ID

        return createMultipartUploadResponse.result().uploadId();
    }

    /**
     * 获取文件上传的预签名 URL。
     *
     * @param bucketName 桶名称
     * @param objectName 文件全路径名称
     * @return 文件上传的预签名 URL
     */
    private String generatePresignedObjectUrl(String bucketName, String objectName) throws Exception {
        return pearlMinioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(1, TimeUnit.DAYS) // URL 过期时间 1 天
                        .build());
    }

    /**
     * @param uploadId   上传id
     * @param bucketName 存储桶名
     * @param objectName 文件名
     * @param partNumber 分片号
     * @return
     * @throws Exception
     */
    private String generatePresignedObjectUrl(String uploadId, String bucketName, String objectName, Integer partNumber) throws Exception {
        return pearlMinioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(1, TimeUnit.DAYS) // URL 过期时间 1 天
                        .extraQueryParams(Map.of(
                                "uploadId", uploadId,
                                "partNumber", String.valueOf(partNumber)
                        ))
                        .build());

    }

    /**
     * 获取⽂件分享链接，带有过期时间
     *
     * @param bucketName    bucket名称
     * @param expirySeconds 过期时间,预签名URL的默认过期时间为7天（单位：秒）
     * @param path          文件在桶中的路径或目录(例：2024/03/08/)
     * @param filename      文件名(包含后缀，例：test.jpg)
     * @return 分享链接
     */
    public String getObjectShareUrl(String bucketName, int expirySeconds, String path, String filename) {
        if (expirySeconds <= 0) {
            expirySeconds = GetPresignedObjectUrlArgs.DEFAULT_EXPIRY_TIME;
        }
        GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(getObjectName(path, filename))
                .expiry(expirySeconds, TimeUnit.SECONDS).build();
        try {
            return pearlMinioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
        } catch (Exception e) {
            log.error("获取文件分享链接异常, bucketName {},path {},filename {}", bucketName, path, filename, e);
            return null;
        }
    }

    /**
     * 生成文件在存储桶中的完整路径
     *
     * @param path     文件路径,例如: "2024/03/08/"
     * @param filename 文件名,例如: "test.jpg"
     * @return 完整的对象名称
     */
    public String getObjectName(String path, String filename) {
        if (StrUtil.isEmpty(filename)) {
            filename = IdUtil.simpleUUID();
        }

        if (StrUtil.isEmpty(path)) {
            return filename;
        }

        return StrUtil.endWith(path, StrPool.C_SLASH) ? path + filename : path + StrPool.C_SLASH + filename;
    }

    /**
     * 根据当前日期生成路径，例如：2025/01/25/
     *
     * @return 按当前日期生成的路径
     */
    public String generatePath() {
        // 提取日期格式为常量
        final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
        // 拼接路径
        return LocalDate.now().format(DATE_FORMATTER) + "/";
    }

    /**
     * 获取每个分片上传的预签名 URL 列表。
     *
     * @param uploadId   上传 ID
     * @param partCount  分片数量
     * @param bucketName 桶名称
     * @param objectName 文件全路径名称
     * @return 分片上传的预签名 URL 映射，key 为 "chunk_分片下标"，value 为 URL
     * @throws IllegalArgumentException 参数校验异常
     * @throws RuntimeException         URL 生成异常
     */
    private Map<String, String> generatePresignedObjectUrls(String uploadId, String bucketName, String objectName, Integer partCount) throws Exception {
        if (partCount <= 0) {
            throw new IllegalArgumentException("分片数量必须大于 0");
        }
        if (CharSequenceUtil.isBlank(uploadId) || CharSequenceUtil.isBlank(bucketName) || CharSequenceUtil.isBlank(objectName)) {
            throw new IllegalArgumentException("参数 uploadId、bucketName 和 objectName 不能为空");
        }

        Map<String, String> urlsMap = new HashMap<>(partCount);
        for (int partNumber = 1; partNumber <= partCount; partNumber++) {
            try {
                String url = generatePresignedObjectUrl(uploadId, bucketName, objectName, partNumber);
                urlsMap.put("chunk_" + (partNumber - 1), url);
            } catch (Exception e) {
                log.error("生成分片上传 URL 时发生错误: partNumber={}, bucketName={}, objectName={}", partNumber, bucketName, objectName, e);
                throw new RuntimeException("生成分片上传 URL 失败", e);
            }
        }
        return urlsMap;
    }

    /**
     * 检查文件是否已存在。
     *
     * @return 是否存在
     */
    public boolean checkObjectExists(OssFile ossFile) {
        String fileName = ossFile.getFileName();
        String bucketName = ossFile.getBucketName();
        try {
            pearlMinioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(fileName).build());
            return true; // 文件存在
        } catch (Exception e) {
            return false; // 文件不存在
        }
    }


    /**
     * 获取文件下载地址
     *
     * @param bucketName 桶名称
     * @param objectName 文件名
     * @return
     */
    public String getFilePath(String bucketName, String objectName) {
        try {
            return pearlMinioClient.generateFileUrl(bucketName, objectName, minioProperties.getLinkExpiry());
        } catch (Exception e) {
            log.error("生成文件预签名地址错误，桶名称：{} 文件名：{}", bucketName, objectName, e);
        }
        return null;
        //  return StrUtil.format("{}/{}/{}", minioProperties.getEndpoint(), bucketName, objectName);//文件访问路径
    }

    /**
     * 根据文件类型获取或创建对应的存储桶
     * 存储桶名称格式: {年份}-{文件类型}, 例如: 2024-images
     *
     * @param fileType 文件类型
     * @return 存储桶名称
     */
    public String getOrCreateBucketByFileType(String fileType) {
        String bucketByFileSuffix = StorageBucketEnum.getBucketByFileSuffix(fileType);
        String currentYear = String.valueOf(LocalDate.now().getYear());

        if (StringUtils.isNotEmpty(bucketByFileSuffix) && !bucketByFileSuffix.equals("*")) {
            try {
                String bucketNameWithYear = currentYear + "-" + bucketByFileSuffix.toLowerCase();
                return createBucket(bucketNameWithYear);
            } catch (Exception e) {
                log.error("获取存储桶名称失败, 文件类型: {}", bucketByFileSuffix, e);
            }
        }
        return currentYear + "-" + fileType;
    }

    /**
     * 支持断点续传的文件下载
     *
     * @param reqOssFile 文件信息对象
     * @param range      HTTP Range 头的值
     * @return 文件下载响应
     */
    public ResponseEntity download(OssFile reqOssFile, String range) {
        String bucketName = reqOssFile.getBucketName();
        String fileName = reqOssFile.getFileName();

        try {
            // 获取文件信息
            StatObjectResponse stat = pearlMinioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(fileName).build()
            ).get();
            long fileSize = stat.size();

            long start = 0, end = fileSize - 1; // 默认下载整个文件

            // 如果 Range 请求头存在，解析范围
            if (range != null && range.startsWith("bytes=")) {
                String[] ranges = range.replace("bytes=", "").split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            }

            // 校验范围合法性
            if (start < 0 || end >= fileSize || start > end) {
                throw new IllegalArgumentException("Invalid Range Header");
            }

            // 获取文件数据流
            InputStream inputStream = pearlMinioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .offset(start)
                            .length(end - start + 1)
                            .build()
            ).get();

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start + 1));
            headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);

            // 返回响应
            return ResponseEntity.status(range == null ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));

        } catch (IllegalArgumentException e) {
            log.error("Invalid Range Header: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        } catch (Exception e) {
            log.error("Error occurred while downloading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    public void uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        try {
            pearlMinioClient.uploadFile(bucketName, objectName, contentType, inputStream);
            log.info("文件已上传: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("上传文件时出错: {}/{}", bucketName, objectName, e);
        }
    }

}
