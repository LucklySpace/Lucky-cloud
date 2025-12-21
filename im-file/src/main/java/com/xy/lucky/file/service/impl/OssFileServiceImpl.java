package com.xy.lucky.file.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.xy.lucky.file.domain.OssFile;
import com.xy.lucky.file.domain.OssFileUploadProgress;
import com.xy.lucky.file.domain.po.OssFilePo;
import com.xy.lucky.file.domain.vo.FileChunkVo;
import com.xy.lucky.file.domain.vo.FileVo;
import com.xy.lucky.file.enums.BoolEnum;
import com.xy.lucky.file.exception.FileException;
import com.xy.lucky.file.mapper.FileVoMapper;
import com.xy.lucky.file.mapper.OssFileEntityMapper;
import com.xy.lucky.file.repository.OssFileRepository;
import com.xy.lucky.file.service.OssFileService;
import com.xy.lucky.file.util.MinioUtils;
import com.xy.lucky.file.util.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * OSS文件服务实现类
 * 处理文件上传、合并、下载等业务逻辑
 */
@Slf4j
@Service
public class OssFileServiceImpl implements OssFileService {

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private OssFileRepository ossFileRepository;

    @Resource
    private OssFileEntityMapper ossFileEntityMapper;

    @Resource
    private FileVoMapper fileVoMapper;

    /**
     * 获取文件上传进度
     *
     * @param identifier 文件唯一标识
     * @return 文件上传进度信息
     */
    @Override
    public OssFileUploadProgress getMultipartUploadProgress(String identifier) {
        log.info("查询文件上传进度 - identifier:{}", identifier);

        // 读取上传会话（优先 Redis，回退 DB）
        OssFile ossFile = getOssFileByIdentifier(identifier);
        OssFileUploadProgress uploadProgress = new OssFileUploadProgress();

        // 新文件：尚未开始上传
        if (ObjectUtils.isEmpty(ossFile)) {
            log.info("文件未开始上传 - identifier:{}", identifier);
            uploadProgress.setIsNew(BoolEnum.YES);
            return uploadProgress;
        }

        Integer isFinish = ossFile.getIsFinish();
        uploadProgress.setIsFinish(isFinish);

        // 已完成上传：直接返回外网访问地址
        if (isFinish.equals(BoolEnum.YES)) {
            String filePath = minioUtils.getFilePath(ossFile.getBucketName(), ossFile.getObjectKey());
            log.info("文件已完成上传 - identifier:{}, path:{}", identifier, filePath);
            uploadProgress.setPath(filePath);
            return uploadProgress;
        }

        // 进行分片进度查询
        log.info("获取分片上传进度 - identifier:{}", identifier);
        return minioUtils.getMultipartUploadProgress(ossFile, uploadProgress);
    }

    /**
     * 初始化分片上传
     *
     * @param reqOssFile 文件信息对象
     * @return 上传初始化结果, 包含uploadId和预签名URL
     */
    @Override
    public FileChunkVo initMultiPartUpload(OssFile reqOssFile) {
        String identifier = reqOssFile.getIdentifier();
        log.info("初始化文件上传 - identifier:{}, fileName:{}", identifier, reqOssFile.getFileName());

        OssFile existingFile = getOssFileByIdentifier(identifier);
        if (existingFile != null) {
            throw new FileException("文件已在上传中 - identifier:" + identifier);
        }

        FileChunkVo result;

        // 单分片走直传，多分片走分片
        if (reqOssFile.getPartNum() == 1) {
            log.info("使用单文件上传 - identifier:{}", identifier);
            result = minioUtils.initUpload(reqOssFile);
        } else {
            log.info("使用分片上传 - identifier:{}, partNum:{}", identifier, reqOssFile.getPartNum());
            result = minioUtils.initMultiPartUpload(reqOssFile);
        }

        // 缓存上传会话（TTL）
        saveOssFileToRedis(reqOssFile);

        log.info("上传分片初始化完成 - identifier:{}", identifier);

        return result;
    }

    @Override
    public FileVo mergeMultipartUpload(String identifier) {
        log.info("[分片合并] 开始合并分片，identifier={}", identifier);

        OssFile ossFile = getOssFileByIdentifier(identifier);
        if (ossFile == null) {
            throw new FileException("[分片合并] 文件未发起过分片上传任务，identifier=" + identifier);
        }
        if (BoolEnum.YES.equals(ossFile.getIsFinish())) {
            throw new FileException("[分片合并] 文件已完成合并，identifier=" + identifier);
        }

        // 合并到最终对象
        String path = minioUtils.mergeOssFileUpload(ossFile);

        // 标记完成并持久化记录
        ossFile.setIsFinish(BoolEnum.YES);

        saveOssFileToRedis(ossFile);

        tryPersist(ossFile, path);

        log.info("[分片合并] 合并完成，identifier={}", identifier);
        return fileVoMapper.toVo(ossFile);
    }

    @Override
    public FileVo isExits(String identifier) {
        log.info("[文件存在检查] 开始检查文件是否存在，identifier={}", identifier);

        OssFile ossFile = getOssFileByIdentifier(identifier);

        if (ossFile != null && minioUtils.checkObjectExists(ossFile)) {
            String filePath = minioUtils.getFilePath(ossFile.getBucketName(), ossFile.getObjectKey());
            ossFile.setPath(filePath);
            log.info("[文件存在检查] 文件已存在，identifier={}，filePath={}", identifier, filePath);
            OssFilePo entity = ossFileEntityMapper.toEntity(ossFile);
            entity.setPath(filePath);
            return fileVoMapper.toVo(entity);
        }

        log.warn("[文件存在检查] 文件不存在，identifier={}", identifier);
        throw new FileException("文件不存在");
    }

    @Override
    public FileVo uploadFile(String identifier, MultipartFile file) {

        log.info("[文件上传] 开始上传文件");

        // 根据文件后缀选择存储桶，避免使用 MIME 类型导致非法桶名
        String originalFilename = file.getOriginalFilename();

        String suffix = null;

        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        String bucketName = minioUtils.getOrCreateBucketByFileType(suffix != null ? suffix : "other");

        // 生成对象名（使用日期路径+原始文件名）
        String objectName = minioUtils.getObjectName(minioUtils.generatePath(), originalFilename);

        try {
            log.info("[文件上传] 存储桶：{}，对象名：{}", bucketName, objectName);

            // 上传文件
            minioUtils.uploadFile(bucketName, objectName, file.getInputStream(), file.getContentType());

            // 缓存上传会话（TTL）
            OssFile ossFile = OssFile.builder()
                    .identifier(identifier)
                    .fileName(originalFilename)
                    .fileType(file.getContentType())
                    .bucketName(bucketName)
                    .objectKey(objectName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .partNum(1)
                    .isFinish(BoolEnum.YES)
                    .path(minioUtils.getPublicFilePath(bucketName, objectName)).build();

            saveOssFileToRedis(ossFile);

            tryPersist(ossFile, ossFile.getPath());

            return fileVoMapper.toVo(ossFile);
        } catch (Exception e) {
            log.error("[文件上传] 文件上传失败", e);
            throw new FileException("文件上传失败");
        }
    }

    @Override
    public ResponseEntity<?> downloadFile(String identifier, String range) {
        log.info("[文件下载] 开始下载文件，identifier={}，range={}", identifier, range);

        // 查询文件是否已经上传
        OssFile ossFile = getOssFileByIdentifier(identifier);

        if (ossFile != null && minioUtils.checkObjectExists(ossFile)) {
            log.info("[文件下载] 文件存在，开始下载，identifier={}", identifier);
            return minioUtils.download(ossFile, range);
        }

        log.warn("[文件下载] 文件不存在，identifier={}", identifier);
        return ResponseEntity.notFound().build();
    }


    /**
     * 获取文件信息
     *
     * @param identifier 文件md5
     * @return 文件信息
     */
    private OssFile getOssFileByIdentifier(String identifier) {
        log.debug("[文件获取] 从 Redis 中获取文件，identifier={}", identifier);

        String objStr = redisUtils.get(identifier);
        if (!StringUtils.hasText(objStr)) {
            log.debug("[文件获取] 文件不存在于 Redis 中，identifier={}", identifier);
            return findFromDb(identifier);
        }

        return JSONObject.parseObject(objStr, OssFile.class);
    }

    /**
     * 保存文件信息
     *
     * @param ossFile 文件信息
     */
    private void saveOssFileToRedis(OssFile ossFile) {
        log.debug("[文件保存] 文件信息保存成功，identifier={}", ossFile.getIdentifier());
        redisUtils.saveTimeout(ossFile.getIdentifier(), JSONObject.toJSONString(ossFile), 30, TimeUnit.MINUTES);
    }


    /**
     * 从数据库中获取文件信息
     *
     * @param identifier 文件md5
     * @return 文件信息
     */
    private OssFile findFromDb(String identifier) {
        return ossFileRepository.findByIdentifier(identifier)
                .map(ossFileEntityMapper::toDomain)
                .orElse(null);
    }

    /**
     * 尝试持久化文件信息
     *
     * @param ossFile 文件信息
     * @param path    文件路径
     */
    private void tryPersist(OssFile ossFile, String path) {
        try {
            OssFilePo entity = ossFileEntityMapper.toEntity(ossFile);
            if (path != null) {
                entity.setPath(path);
            }
            ossFileRepository.findByIdentifier(ossFile.getIdentifier())
                    .ifPresentOrElse(existing -> {
                        entity.setId(existing.getId());
                        ossFileRepository.save(entity);
                    }, () -> ossFileRepository.save(entity));
        } catch (Exception ex) {
            log.warn("持久化文件失败 identifier={}", ossFile.getIdentifier(), ex);
        }
    }
}
