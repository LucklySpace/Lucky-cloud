package com.xy.lucky.oss.service.impl;

import com.xy.lucky.oss.client.OssProperties;
import com.xy.lucky.oss.domain.OssFileUploadProgress;
import com.xy.lucky.oss.domain.mapper.FileVoMapper;
import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.domain.vo.FileChunkVo;
import com.xy.lucky.oss.domain.vo.FileUploadProgressVo;
import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.enums.BoolEnum;
import com.xy.lucky.oss.exception.FileException;
import com.xy.lucky.oss.repository.OssFileRepository;
import com.xy.lucky.oss.service.OssFileService;
import com.xy.lucky.oss.util.FileTypeDetector;
import com.xy.lucky.oss.util.MD5Utils;
import com.xy.lucky.oss.util.OssUtils;
import com.xy.lucky.oss.util.RedisUtils;
import com.xy.lucky.utils.id.IdUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class OssFileServiceS3Impl implements OssFileService {

    private static final int CACHE_TTL_SECONDS = 30 * 60;
    private static final String EXPIRED_CLEANUP_LOCK_KEY = "im:oss:multipart:cleanup:lock";
    private static final int EXPIRED_CLEANUP_LOCK_SECONDS = 55;

    @Resource
    private OssUtils ossUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private OssFileRepository ossFileRepository;

    @Resource
    private FileVoMapper fileVoMapper;

    @Resource
    private OssProperties ossProperties;

    @Resource
    private FileTypeDetector fileTypeDetector;

    @Override
    public FileUploadProgressVo getMultipartUploadProgress(String identifier) {
        log.info("查询文件上传进度 - identifier:{}", identifier);
        OssFilePo ossFilePo = findExistingByIdentifier(identifier);
        OssFileUploadProgress uploadProgress = new OssFileUploadProgress();

        if (Objects.isNull(ossFilePo)) {
            uploadProgress.setIsNew(BoolEnum.YES);
            uploadProgress.setIsFinish(BoolEnum.NO);
            return fileVoMapper.toVo(uploadProgress);
        }

        uploadProgress.setIsNew(BoolEnum.NO);
        Integer isFinish = ossFilePo.getIsFinish();
        uploadProgress.setIsFinish(isFinish);

        if (BoolEnum.YES.equals(isFinish)) {
            String filePath = ossUtils.getPresignedGetUrl(ossFilePo.getBucketName(), ossFilePo.getObjectKey());
            uploadProgress.setPath(filePath);
            return fileVoMapper.toVo(uploadProgress);
        }

        return fileVoMapper.toVo(ossUtils.getMultipartUploadProgress(ossFilePo, uploadProgress));
    }

    @Override
    public FileChunkVo initMultiPartUpload(OssFilePo reqOssFilePo) {
        validateInitInput(reqOssFilePo);
        String identifier = reqOssFilePo.getIdentifier();
        log.info("初始化文件上传 - identifier:{}, fileName:{}", identifier, reqOssFilePo.getFileName());

        OssFilePo existingFile = findExistingByIdentifier(identifier);
        if (existingFile != null) {
            throw new FileException("文件已在上传中 - identifier:" + identifier);
        }

        FileChunkVo result;
        if (reqOssFilePo.getPartNum() == 1) {
            result = ossUtils.initUpload(reqOssFilePo);
        } else {
            result = ossUtils.initMultiPartUpload(reqOssFilePo);
        }

        reqOssFilePo.setUploadId(result.getUploadId());
        reqOssFilePo.setIsFinish(BoolEnum.NO);
        reqOssFilePo.setCreateTime(LocalDateTime.now());
        saveOssFileToRedis(reqOssFilePo);
        tryPersist(reqOssFilePo);
        return result;
    }

    @Override
    public FileVo mergeMultipartUpload(String identifier) {
        OssFilePo ossFilePo = getRequiredFileByIdentifier(identifier);
        if (BoolEnum.YES.equals(ossFilePo.getIsFinish())) {
            throw new FileException("文件已完成合并，identifier=" + identifier);
        }

        String path = ossUtils.mergeOssFileUpload(ossFilePo);
        ossFilePo.setIsFinish(BoolEnum.YES);
        saveOssFileToRedis(ossFilePo);
        tryPersist(ossFilePo);
        FileVo vo = fileVoMapper.toVo(ossFilePo);
        vo.setPath(path);
        return vo;
    }

    @Override
    public FileVo isExits(String identifier) {
        OssFilePo ossFilePo = findExistingByIdentifier(identifier);
        if (ossFilePo != null && ossUtils.checkObjectExists(ossFilePo)) {
            return fileVoMapper.toVo(ossFilePo);
        }
        throw new FileException("文件不存在");
    }

    @Override
    public FileVo uploadFile(String identifier, MultipartFile file) {
        validateUploadInput(identifier, file);
        String originalFilename = file.getOriginalFilename();
        log.info("[文件上传] 开始上传文件 文件名: {}, 文件大小: {}", originalFilename, file.getSize());

        OssFilePo ossFilePoByIdentifier = findExistingByIdentifier(identifier);
        if (Objects.nonNull(ossFilePoByIdentifier)) {
            return fileVoMapper.toVo(ossFilePoByIdentifier);
        }

        FileTypeDetector.DetectedFileType detectedFileType = fileTypeDetector.detect(file);
        fileTypeDetector.validateByScene(detectedFileType, FileTypeDetector.UploadScene.FILE);

        String suffix = detectedFileType.extension();
        String bucketName = ossUtils.getOrCreateBucketByCode(detectedFileType.bucketCode());
        String fileName = IdUtils.randomUUID() + "." + suffix;
        String objectName = ossUtils.getObjectName(ossUtils.generatePath(), fileName);

        try {
            ossUtils.uploadFile(bucketName, objectName, file.getInputStream(), detectedFileType.mimeType());

            OssFilePo ossFilePo = OssFilePo.builder()
                    .identifier(identifier)
                    .fileName(originalFilename)
                    .fileType(detectedFileType.bucketCode())
                    .bucketName(bucketName)
                    .objectKey(objectName)
                    .contentType(detectedFileType.mimeType())
                    .suffix(suffix)
                    .fileSize(file.getSize())
                    .partNum(1)
                    .isFinish(BoolEnum.YES)
                    .build();

            saveOssFileToRedis(ossFilePo);
            tryPersist(ossFilePo);
            return fileVoMapper.toVo(ossFilePo);
        } catch (Exception e) {
            throw new FileException("文件上传失败");
        }
    }

    @Override
    public ResponseEntity<?> downloadFile(String identifier, String range) {
        if (!StringUtils.hasText(identifier)) {
            return ResponseEntity.badRequest().build();
        }
        OssFilePo ossFilePo = findExistingByIdentifier(identifier);
        if (ossFilePo != null && ossUtils.checkObjectExists(ossFilePo)) {
            return ossUtils.download(ossFilePo, range);
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public FileVo getFileMd5(MultipartFile file) {
        return FileVo.builder().key(MD5Utils.getMD5(file)).build();
    }

    @Override
    public FileVo getPresignedPutUrl(String identifier) {
        OssFilePo file = getRequiredFileByIdentifier(identifier);
        return FileVo.builder()
                .key(identifier)
                .path(ossUtils.getPresignedUrl(file.getBucketName(), file.getObjectKey(), ossProperties.getPresignedUrlExpiry()))
                .build();
    }

    /**
     * 校验上传请求参数与文件摘要。
     *
     * @param identifier 文件 MD5
     * @param file       上传文件
     */
    private void validateUploadInput(String identifier, MultipartFile file) {
        if (file == null || file.isEmpty() || !StringUtils.hasText(identifier)) {
            throw new FileException("文件或文件md5不能为空");
        }
        MD5Utils.checkMD5(identifier, file);
    }

    private void validateInitInput(OssFilePo reqOssFilePo) {
        if (reqOssFilePo == null) {
            throw new FileException("初始化参数不能为空");
        }
        if (!StringUtils.hasText(reqOssFilePo.getIdentifier())) {
            throw new FileException("文件md5不能为空");
        }
        if (!StringUtils.hasText(reqOssFilePo.getFileName())) {
            throw new FileException("文件名不能为空");
        }
        if (reqOssFilePo.getPartNum() == null || reqOssFilePo.getPartNum() < 1) {
            throw new FileException("分片数量必须大于0");
        }
    }

    private OssFilePo findExistingByIdentifier(String identifier) {
        OssFilePo ossFilePo = Optional.ofNullable((OssFilePo) redisUtils.get(identifier))
                .or(() -> ossFileRepository.findByIdentifier(identifier))
                .orElse(null);
        if (isExpiredPendingUpload(ossFilePo)) {
            cleanupExpiredUpload(ossFilePo, "on-read");
            return null;
        }
        keepAlivePendingUpload(ossFilePo);
        return ossFilePo;
    }

    private OssFilePo getRequiredFileByIdentifier(String identifier) {
        return Optional.ofNullable(findExistingByIdentifier(identifier))
                .orElseThrow(() -> new FileException("文件不存在"));
    }

    private void saveOssFileToRedis(OssFilePo ossFilePo) {
        redisUtils.set(ossFilePo.getIdentifier(), ossFilePo, CACHE_TTL_SECONDS);
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanupExpiredMultipartUploads() {
        boolean locked = redisUtils.setIfAbsent(EXPIRED_CLEANUP_LOCK_KEY, "1", EXPIRED_CLEANUP_LOCK_SECONDS);
        if (!locked) {
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusSeconds(CACHE_TTL_SECONDS);
            List<OssFilePo> expiredTasks = ossFileRepository
                    .findTop200ByIsFinishAndCreateTimeBeforeOrderByCreateTimeAsc(BoolEnum.NO, deadline);
            for (OssFilePo task : expiredTasks) {
                cleanupExpiredUpload(task, "scheduled");
            }
        } catch (Exception e) {
            log.warn("清理过期分片任务失败: {}", e.getMessage());
        } finally {
            redisUtils.del(EXPIRED_CLEANUP_LOCK_KEY);
        }
    }

    private void tryPersist(OssFilePo ossFilePo) {
        try {
            ossFileRepository.findByIdentifier(ossFilePo.getIdentifier())
                    .map(OssFilePo::getId)
                    .ifPresent(ossFilePo::setId);
            ossFileRepository.save(ossFilePo);
        } catch (Exception ex) {
            log.warn("保存文件记录失败 - identifier:{}, error:{}", ossFilePo.getIdentifier(), ex.getMessage());
        }
    }

    private boolean isExpiredPendingUpload(OssFilePo ossFilePo) {
        if (ossFilePo == null || BoolEnum.YES.equals(ossFilePo.getIsFinish())) {
            return false;
        }
        LocalDateTime activeTime = ossFilePo.getCreateTime();
        if (activeTime == null) {
            return false;
        }
        return activeTime.isBefore(LocalDateTime.now().minusSeconds(CACHE_TTL_SECONDS));
    }

    private void keepAlivePendingUpload(OssFilePo ossFilePo) {
        if (ossFilePo == null || BoolEnum.YES.equals(ossFilePo.getIsFinish())) {
            return;
        }
        ossFilePo.setCreateTime(LocalDateTime.now());
        saveOssFileToRedis(ossFilePo);
        tryPersist(ossFilePo);
    }

    private void cleanupExpiredUpload(OssFilePo ossFilePo, String source) {
        if (ossFilePo == null) {
            return;
        }
        try {
            ossUtils.abortMultipartUpload(ossFilePo);
        } catch (Exception e) {
            log.warn("中止过期分片任务失败 - source:{}, identifier:{}, error:{}", source, ossFilePo.getIdentifier(), e.getMessage());
        }
        redisUtils.del(ossFilePo.getIdentifier());
        try {
            ossFileRepository.deleteByIdentifier(ossFilePo.getIdentifier());
        } catch (Exception e) {
            log.warn("删除过期分片任务记录失败 - source:{}, identifier:{}, error:{}", source, ossFilePo.getIdentifier(), e.getMessage());
        }
    }
}
