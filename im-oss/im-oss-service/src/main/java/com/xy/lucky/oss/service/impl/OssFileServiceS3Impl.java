package com.xy.lucky.oss.service.impl;

import com.xy.lucky.oss.domain.OssFileUploadProgress;
import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.domain.vo.FileChunkVo;
import com.xy.lucky.oss.domain.vo.FileUploadProgressVo;
import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.enums.BoolEnum;
import com.xy.lucky.oss.enums.StorageBucketEnum;
import com.xy.lucky.oss.exception.FileException;
import com.xy.lucky.oss.mapper.FileVoMapper;
import com.xy.lucky.oss.repository.OssFileRepository;
import com.xy.lucky.oss.service.OssFileService;
import com.xy.lucky.oss.util.MD5Utils;
import com.xy.lucky.oss.util.OssUtils;
import com.xy.lucky.oss.util.RedisUtils;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.json.JacksonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OssFileServiceS3Impl implements OssFileService {

    @Resource
    private OssUtils ossUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private OssFileRepository ossFileRepository;

    @Resource
    private FileVoMapper fileVoMapper;

    @Override
    public FileUploadProgressVo getMultipartUploadProgress(String identifier) {
        log.info("查询文件上传进度 - identifier:{}", identifier);
        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);
        OssFileUploadProgress uploadProgress = new OssFileUploadProgress();

        if (ObjectUtils.isEmpty(ossFilePo)) {
            uploadProgress.setIsNew(BoolEnum.YES);
            return fileVoMapper.toVo(uploadProgress);
        }

        Integer isFinish = ossFilePo.getIsFinish();
        uploadProgress.setIsFinish(isFinish);

        if (BoolEnum.YES.equals(isFinish)) {
            String filePath = ossUtils.getFilePath(ossFilePo.getBucketName(), ossFilePo.getObjectKey());
            uploadProgress.setPath(filePath);
            return fileVoMapper.toVo(uploadProgress);
        }

        return fileVoMapper.toVo(ossUtils.getMultipartUploadProgress(ossFilePo, uploadProgress));
    }

    @Override
    public FileChunkVo initMultiPartUpload(OssFilePo reqOssFilePo) {
        String identifier = reqOssFilePo.getIdentifier();
        log.info("初始化文件上传 - identifier:{}, fileName:{}", identifier, reqOssFilePo.getFileName());

        OssFilePo existingFile = getOssFileByIdentifier(identifier);
        if (existingFile != null) {
            throw new FileException("文件已在上传中 - identifier:" + identifier);
        }

        FileChunkVo result;
        if (reqOssFilePo.getPartNum() == 1) {
            result = ossUtils.initUpload(reqOssFilePo);
        } else {
            result = ossUtils.initMultiPartUpload(reqOssFilePo);
        }

        saveOssFileToRedis(reqOssFilePo);
        return result;
    }

    @Override
    public FileVo mergeMultipartUpload(String identifier) {
        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);
        if (ossFilePo == null) {
            throw new FileException("文件未发起过分片上传任务，identifier=" + identifier);
        }
        if (BoolEnum.YES.equals(ossFilePo.getIsFinish())) {
            throw new FileException("文件已完成合并，identifier=" + identifier);
        }

        String path = ossUtils.mergeOssFileUpload(ossFilePo);
        ossFilePo.setIsFinish(BoolEnum.YES);
        saveOssFileToRedis(ossFilePo);
        tryPersist(ossFilePo, path);
        return fileVoMapper.toVo(ossFilePo);
    }

    @Override
    public FileVo isExits(String identifier) {
        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);
        if (ossFilePo != null && ossUtils.checkObjectExists(ossFilePo)) {
            String filePath = ossUtils.getFilePath(ossFilePo.getBucketName(), ossFilePo.getObjectKey());
            ossFilePo.setPath(filePath);
            return fileVoMapper.toVo(ossFilePo);
        }
        throw new FileException("文件不存在");
    }

    @Override
    public FileVo uploadFile(String identifier, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("[文件上传] 开始上传文件 文件名: {}, 文件大小: {}", originalFilename, file.getSize());

        if (file == null || file.isEmpty() || !StringUtils.hasText(identifier)) {
            throw new FileException("文件或文件md5不能为空");
        }
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new FileException("文件名格式错误");
        }

        MD5Utils.checkMD5(identifier, file);

        OssFilePo ossFilePoByIdentifier = getOssFileByIdentifier(identifier);
        if (Objects.nonNull(ossFilePoByIdentifier)) {
            return fileVoMapper.toVo(ossFilePoByIdentifier);
        }

        String suffix = StorageBucketEnum.getSuffix(originalFilename);
        String bucketName = ossUtils.getOrCreateBucketByFileName(originalFilename);
        String fileName = IdUtils.randomUUID() + "." + suffix;
        String objectName = ossUtils.getObjectName(ossUtils.generatePath(), fileName);

        try {
            ossUtils.uploadFile(bucketName, objectName, file.getInputStream(), file.getContentType());

            OssFilePo ossFilePo = OssFilePo.builder()
                    .identifier(identifier)
                    .fileName(originalFilename)
                    .fileType(StorageBucketEnum.getBucketCodeByFilename(originalFilename))
                    .bucketName(bucketName)
                    .objectKey(objectName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .partNum(1)
                    .isFinish(BoolEnum.YES)
                    .path(ossUtils.getFilePath(bucketName, objectName)).build();

            saveOssFileToRedis(ossFilePo);
            tryPersist(ossFilePo, ossFilePo.getPath());
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
        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);
        if (ossFilePo != null && ossUtils.checkObjectExists(ossFilePo)) {
            return ossUtils.download(ossFilePo, range);
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public FileVo getFileMd5(MultipartFile file) {
        return FileVo.builder().identifier(MD5Utils.getMD5(file)).build();
    }

    private OssFilePo getOssFileByIdentifier(String identifier) {
        String objStr = redisUtils.get(identifier);
        if (!StringUtils.hasText(objStr)) {
            return findFromDb(identifier);
        }
        return JacksonUtils.parseObject(objStr, OssFilePo.class);
    }

    private void saveOssFileToRedis(OssFilePo ossFilePo) {
        redisUtils.saveTimeout(ossFilePo.getIdentifier(), JacksonUtils.toJSONString(ossFilePo), 30, TimeUnit.MINUTES);
    }

    private OssFilePo findFromDb(String identifier) {
        return ossFileRepository.findByIdentifier(identifier).orElse(null);
    }

    private void tryPersist(OssFilePo ossFilePo, String path) {
        try {
            if (path != null) {
                ossFilePo.setPath(path);
            }
            ossFileRepository.findByIdentifier(ossFilePo.getIdentifier())
                    .ifPresentOrElse(existing -> {
                        ossFilePo.setId(existing.getId());
                        ossFileRepository.save(ossFilePo);
                    }, () -> ossFileRepository.save(ossFilePo));
        } catch (Exception ignored) {
        }
    }
}
